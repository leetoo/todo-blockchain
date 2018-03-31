package exchange

import akka.actor.{ActorRef, Props}
import exchange.mining.{SimpleForger, DeeSettings}
import exchange.model._
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute}
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizerRef
import scorex.core.network.message.MessageSpec
import scorex.core.settings.ScorexSettings
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.io.Source
import scala.language.postfixOps

class SimpleApp(val settingsFilename: String) extends Application {

  override type P = PublicKey25519Proposition
  override type TX = BaseEvent
  override type PMOD = SimpleBlock
  override type NVHT = SimpleNodeViewHolder

  private val hybridSettings = DeeSettings.read(Some(settingsFilename))

  implicit override lazy val settings: ScorexSettings = DeeSettings.read(Some(settingsFilename)).scorexSettings

  log.debug(s"Starting application with settings \n$settings")

  override protected lazy val additionalMessageSpecs: Seq[MessageSpec[_]] = Seq(DeeSyncInfoMessageSpec)

  override val nodeViewHolderRef: ActorRef = DeeNodeViewHolderRef(settings, hybridSettings.mining, timeProvider)

  override val apiRoutes: Seq[ApiRoute] = Seq(
    NodeViewApiRoute[P, TX](settings.restApi, nodeViewHolderRef)
  )

  override val swaggerConfig: String = Source.fromResource("api/testApi.yaml").getLines.mkString("\n")

  val forger = actorSystem.actorOf(Props(new SimpleForger(nodeViewHolderRef, hybridSettings.mining, timeProvider)))

  override val localInterface: ActorRef = DeeLocalInterfaceRef(nodeViewHolderRef, forger, hybridSettings.mining)

  override val nodeViewSynchronizer: ActorRef =
    actorSystem.actorOf(NodeViewSynchronizerRef.props[P, TX, SimpleSyncInfo, DeeSyncInfoMessageSpec.type, PMOD, SimpleBlockchain, SimpleCommandMemPool](networkControllerRef, nodeViewHolderRef, localInterface,
      DeeSyncInfoMessageSpec, settings.network, timeProvider))

}

object SimpleApp extends App {
  val settingsFilename = args.headOption.getOrElse("settings.conf")
  new SimpleApp(settingsFilename).run()
}
