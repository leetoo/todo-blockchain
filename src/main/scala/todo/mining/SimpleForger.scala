package todo.mining

import akka.actor.{Actor, ActorRef}
import todo.SimpleCommandMemPool
import todo.model._
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedModifier
import scorex.core.NodeViewHolder.CurrentView
import scorex.core.NodeViewHolder.ReceivableMessages.GetDataFromCurrentView
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.{PrivateKey25519, PrivateKey25519Companion}
import scorex.core.utils.{NetworkTimeProvider, ScorexLogging}
import scorex.crypto.hash.Blake2b256

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SimpleForger(viewHolderRef: ActorRef, forgerSettings: MiningSettings, timeProvider: NetworkTimeProvider) extends Actor with ScorexLogging {

  import SimpleForger._

  //set to true for initial generator
  private var forging = forgerSettings.offlineGeneration

  private val hash = Blake2b256

  override def preStart(): Unit = {
    if (forging) context.system.scheduler.scheduleOnce(1.second)(self ! Forge)
  }

  private def bounded(value: BigInt, min: BigInt, max: BigInt): BigInt =
    if (value < min) min else if (value > max) max else value

  private def calcBaseTarget(
                              lastBlock: SimpleBlock,
                              currentTime: Long
  ): Long = {
    val eta = currentTime - lastBlock.timestamp
    val prevBt = BigInt(lastBlock.baseTarget)
    val t0: BigInt = bounded(prevBt * eta / forgerSettings.targetBlockDelay.toMillis, prevBt / 2, prevBt * 2)
    bounded(t0, 1, Long.MaxValue).toLong
  }

  protected def calcTarget(
                            lastBlock: SimpleBlock,
                            boxOpt: Option[SimpleBox]
  ): BigInt = {
    val eta = (timeProvider.time() - lastBlock.timestamp) / 1000 //in seconds
    //we are ignoring original Box value
    val balance = boxOpt.map(_ => 50000000L ).getOrElse(0L)
    BigInt(lastBlock.baseTarget) * eta * balance
  }

  private def calcGeneratorSignature(lastBlock: SimpleBlock, generator: PublicKey25519Proposition) =
    hash(lastBlock.generationSignature ++ generator.pubKeyBytes)

  private def calcHit(lastBlock: SimpleBlock, generator: PublicKey25519Proposition): BigInt =
    BigInt(1, calcGeneratorSignature(lastBlock, generator).take(8))

  override def receive: Receive = {
    case StartMining =>
      forging = true
      context.system.scheduler.scheduleOnce(forgerSettings.blockGenerationDelay)(self ! Forge)

    case StopMining =>
      forging = false

    case info: RequiredForgingInfo =>
      val lastBlock = info.lastBlock
      log.info(s"Trying to generate a new block on top of $lastBlock")
      lazy val toInclude = info.toInclude

      val generatedBlocks = info.gbs.flatMap { gb =>
        val generator = gb._1
        val hit = calcHit(lastBlock, generator)
        val target = calcTarget(lastBlock, gb._2)
        if (hit < target) {
          Some {
            val timestamp = timeProvider.time()
            val bt = BaseTarget @@ calcBaseTarget(lastBlock, timestamp)
            val secret = gb._3
            val gs = GenerationSignature @@ Array[Byte]()

            val unsigned: SimpleBlock = SimpleBlock(lastBlock.id, timestamp, gs, bt, generator, toInclude)
            val signature = PrivateKey25519Companion.sign(secret, unsigned.serializer.messageToSign(unsigned))
            val signedBlock = unsigned.copy(generationSignature = GenerationSignature @@ signature.signature)
            log.info(s"Generated new block: ${signedBlock.json.noSpaces}")
            LocallyGeneratedModifier[SimpleBlock](signedBlock)
          }
        } else {
          None
        }
      }
      generatedBlocks.foreach(localModifier => viewHolderRef ! localModifier)
      context.system.scheduler.scheduleOnce(forgerSettings.blockGenerationDelay)(self ! Forge)

    case Forge =>
      viewHolderRef ! SimpleForger.getRequiredData
  }
}

object SimpleForger {
  //should be a part of consensus, but for our app is okay
  val TransactionsInBlock = 100

  val getRequiredData: GetDataFromCurrentView[SimpleBlockchain, SimpleState, SimpleWallet, SimpleCommandMemPool, RequiredForgingInfo] = {
    val f: CurrentView[SimpleBlockchain, SimpleState, SimpleWallet, SimpleCommandMemPool] => RequiredForgingInfo = {
      view: CurrentView[SimpleBlockchain, SimpleState, SimpleWallet, SimpleCommandMemPool] =>
        val toInclude = view.state.filterValid(view.pool.take(TransactionsInBlock).toSeq)
        val lastBlock = view.history.lastBlock.getOrElse(throw new Exception("Previous block not exist"))

        //Forger try to forge block for each key from vault. For simplewallet we have only one key :)
        val gbs: Seq[(PublicKey25519Proposition, Option[SimpleBox], PrivateKey25519)] = {
          view.vault.publicKeys.map { pk =>
            val boxOpt: Option[SimpleBox] = view.state.boxesOf(pk).headOption
            val secret: PrivateKey25519 = view.vault.secretByPublicImage(pk).get
            (pk, boxOpt, secret)
          }.toSeq
        }

        RequiredForgingInfo(toInclude, lastBlock, gbs)
    }
    GetDataFromCurrentView[SimpleBlockchain, SimpleState, SimpleWallet, SimpleCommandMemPool, RequiredForgingInfo](f)
  }

  case class RequiredForgingInfo(
                                  toInclude: Seq[BaseEvent],
                                  lastBlock: SimpleBlock,
                                  gbs: Seq[(PublicKey25519Proposition, Option[SimpleBox], PrivateKey25519)]
  )

  case object StartMining

  case object StopMining

  case object Forge

}