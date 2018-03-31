package exchange.model

import java.nio.ByteBuffer

import com.google.common.primitives.Longs
import exchange.model.SimpleState.EmptyVersion
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state._
import scorex.core.utils.ScorexLogging
import scorex.crypto.encode.Base58
import scorex.mid.state.BoxMinimalState

import scala.util.{Success, Try}


case class SimpleState(override val version: VersionTag = EmptyVersion,
                       storage: Map[ByteBuffer, SimpleBox] = Map()) extends ScorexLogging
  with BoxMinimalState[PublicKey25519Proposition, SimpleBox, BaseEvent, SimpleBlock, SimpleState] {

  def isEmpty: Boolean = version sameElements EmptyVersion

  def totalBalance: Long = storage.keySet.flatMap(k => storage.get(k).map(_.value.toLong)).sum

  override def toString: String = {
    s"DeeState at ${Base58.encode(version)}\n" + storage.keySet.flatMap(k => storage.get(k)).mkString("\n  ")
  }

  override def boxesOf(p: PublicKey25519Proposition): Seq[SimpleBox] =
    storage.values.filter(b => b.proposition.address == p.address).toSeq

  override def closedBox(boxId: Array[Byte]): Option[SimpleBox] =
    storage.get(ByteBuffer.wrap(boxId))

  override def maxRollbackDepth: Int = 0

  override def rollbackTo(version: VersionTag): Try[SimpleState] = {
    log.warn("Rollback is not implemented")
    Try(this)
  }

  override def applyChanges(change: BoxStateChanges[PublicKey25519Proposition, SimpleBox], newVersion: VersionTag): Try[SimpleState] = Try {
    val rmap = change.toRemove.foldLeft(storage) { case (m, r) => m - ByteBuffer.wrap(r.boxId) }

    val amap = change.toAppend.foldLeft(rmap) { case (m, a) =>
      val b = a.box.ensuring(_.value >= 0)
      m + (ByteBuffer.wrap(b.id) -> b)
    }
    SimpleState(newVersion, amap)
  }

  override type NVCT = SimpleState

  override def validate(transaction: BaseEvent): Try[Unit] = Try{

    //TODO implement me
  }

//    transaction match {
//    case sp: SimplePayment => Try {
//      val b = boxesOf(sp.sender).head
//      (b.value >= Math.addExact(sp.amount, sp.fee)) && (b.nonce + 1 == sp.nonce)
//    }
//  }

//  /**
//    * A Transaction opens existing boxes and creates new ones
//    */
//  def changes(transaction: OfferCommand): Try[TransactionChanges[PublicKey25519Proposition, OfferBox]] = {
//    transaction match {
//      case tx: SimplePayment if !isEmpty => Try {
//        val oldSenderBox = boxesOf(tx.sender).head
//        val oldRecipientBox = boxesOf(tx.recipient).headOption
//        val newRecipientBox = oldRecipientBox.map { oldB =>
//          oldB.copy(nonce = Nonce @@ (oldB.nonce + 1), value = Value @@ Math.addExact(oldB.value, tx.amount))
//        }.getOrElse(OfferBox(tx.recipient, Nonce @@ 0L, Value @@ tx.amount))
//        val newSenderBox = oldSenderBox.copy(nonce = Nonce @@ (oldSenderBox.nonce + 1),
//          value = Value @@ Math.addExact(Math.addExact(oldSenderBox.value, -tx.amount), -tx.fee))
//        val toRemove = Set(oldSenderBox) ++ oldRecipientBox
//        val toAppend = Set(newRecipientBox, newSenderBox)
//        if (!toAppend.forall(_.value >= 0)) throw new Error(s"Trying to create negative output in ${toAppend}")
//
//        TransactionChanges[PublicKey25519Proposition, OfferBox](toRemove, toAppend, tx.fee)
//      }
//      case genesis: SimplePayment if isEmpty => Try {
//        val toAppend: Set[OfferBox] = Set(OfferBox(genesis.recipient, Nonce @@ 0L, Value @@ genesis.amount))
//        TransactionChanges[PublicKey25519Proposition, OfferBox](Set(), toAppend, 0)
//      }
//      case _ => Failure(new Exception("implementation is needed"))
//    }
//  }

  override def changes(block: SimpleBlock): Try[BoxStateChanges[PublicKey25519Proposition, SimpleBox]] =
    Try {
      val initial = (Seq(): Seq[Array[Byte]], Seq(): Seq[SimpleBox], 0L)

//      val (toRemove: Seq[ADKey@unchecked], toAdd: Seq[PublicKey25519NoncedBox], reward) =
//        block.transactions.foldLeft(initial) { case ((sr, sa, f), tx) =>
//          ((sr ++ tx.boxIdsToOpen.toSet).map(id => ADKey @@ id), sa ++ tx.newBoxes.toSet, f + tx.fee)
//        }

      //for PoS forger reward box, we use block Id as a nonce
      val forgerNonce = Nonce @@ Longs.fromByteArray(block.id.take(8))
      val forgerBox = SimpleBox(block.generator, forgerNonce)

      val ops: Seq[BoxStateChangeOperation[PublicKey25519Proposition, SimpleBox]] = Seq(Insertion[PublicKey25519Proposition, SimpleBox](forgerBox))
//        toRemove.map(id => Removal[PublicKey25519Proposition, PublicKey25519NoncedBox](id)) ++
//          toAdd.map(b => Insertion[PublicKey25519Proposition, PublicKey25519NoncedBox](b)) ++

      BoxStateChanges[PublicKey25519Proposition, SimpleBox](ops)
    }
//  Try {
//      val generatorReward = block.transactions.map(_.fee).sum
//      val gen = block.generator
//
//      val txChanges = block.transactions.map(tx => changes(tx)).map(_.get)
//      val toRemove = txChanges.flatMap(_.toRemove).map(_.id).map(id =>
//        Removal[PublicKey25519Proposition, PublicKey25519NoncedBox](id))
//      val toAppendFrom = txChanges.flatMap(_.toAppend)
//      val (generator, withoutGenerator) = toAppendFrom.partition(_.proposition.address == gen.address)
//      val generatorBox: PublicKey25519NoncedBox = (generator ++ boxesOf(gen)).headOption match {
//        case Some(oldBox) =>
//          oldBox.copy(nonce = Nonce @@ (oldBox.nonce + 1), value = Value @@ (oldBox.value + generatorReward))
//        case None =>
//          PublicKey25519NoncedBox(gen, Nonce @@ 1L, Value @@ generatorReward)
//      }
//      val toAppend = (withoutGenerator ++ Seq(generatorBox)).map(b =>
//        Insertion[PublicKey25519Proposition, PublicKey25519NoncedBox](b)).ensuring(_.forall(_.box.value >= 0))
//
//      BoxStateChanges[PublicKey25519Proposition, PublicKey25519NoncedBox](toRemove ++ toAppend)
//    }

  //    Try {
//    val generatorReward = block.transactions.map(_.fee).sum
//    val gen = block.generator
//
//    val txChanges = block.transactions.map(tx => changes(tx)).map(_.get)
//    val toRemove = txChanges.flatMap(_.toRemove).map(_.id).map(id =>
//      Removal[PublicKey25519Proposition, OfferBox](id))
//    val toAppendFrom = txChanges.flatMap(_.toAppend)
//    val (generator, withoutGenerator) = toAppendFrom.partition(_.proposition.address == gen.address)
//    val generatorBox: OfferBox = (generator ++ boxesOf(gen)).headOption match {
//      case Some(oldBox) =>
//        oldBox.copy(nonce = Nonce @@ (oldBox.nonce + 1), value = Value @@ (oldBox.value + generatorReward))
//      case None =>
//        OfferBox(gen, Nonce @@ 1L, Value @@ generatorReward)
//    }
//    val toAppend = (withoutGenerator ++ Seq(generatorBox)).map(b =>
//      Insertion[PublicKey25519Proposition, OfferBox](b)).ensuring(_.forall(_.box.value >= 0))
//
//    BoxStateChanges[PublicKey25519Proposition, OfferBox](toRemove ++ toAppend)
//  }

  override def semanticValidity(tx: BaseEvent): Try[Unit] = Success()

  override def validate(mod: SimpleBlock): Try[Unit] = Try(mod.transactions.foreach(tx => validate(tx).get))
}

object SimpleState {
  val EmptyVersion: VersionTag = VersionTag @@ Array.fill(32)(0: Byte)
}