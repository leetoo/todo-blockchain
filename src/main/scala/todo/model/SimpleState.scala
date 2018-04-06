package todo.model

import java.nio.ByteBuffer

import com.google.common.primitives.Longs
import todo.model.SimpleState.EmptyVersion
import scorex.core.VersionTag
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state._
import scorex.core.utils.ScorexLogging
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58
import scorex.mid.state.BoxMinimalState

import scala.util.{Success, Try}

case class SimpleState(
  override val version: VersionTag = EmptyVersion,
  storage: Map[ByteBuffer, TodoBox] = Map()
) extends ScorexLogging
  with BoxMinimalState[PublicKey25519Proposition, TodoBox, BaseEvent, SimpleBlock, SimpleState] {

  def isEmpty: Boolean = version sameElements EmptyVersion

  def totalBalance: Long = storage.keySet.flatMap(k => storage.get(k).map(_.value.toLong)).sum

  override def toString: String = {
    s"SimpleState at ${Base58.encode(version)}\n" + storage.keySet.flatMap(k => storage.get(k)).mkString("\n  ")
  }

  override def boxesOf(p: PublicKey25519Proposition): Seq[TodoBox] =
    storage.values.filter(b => b.proposition.address == p.address).toSeq

  override def closedBox(boxId: Array[Byte]): Option[TodoBox] =
    storage.get(ByteBuffer.wrap(boxId))

  override def maxRollbackDepth: Int = 0

  override def rollbackTo(version: VersionTag): Try[SimpleState] = {
    log.warn("Rollback is not implemented")
    Try(this)
  }

  override def applyChanges(change: BoxStateChanges[PublicKey25519Proposition, TodoBox], newVersion: VersionTag): Try[SimpleState] = Try {
    val rmap = change.toRemove.foldLeft(storage) { case (m, r) => m - ByteBuffer.wrap(r.boxId) }

    val amap = change.toAppend.foldLeft(rmap) {
      case (m, a) =>
        val b = a.box.ensuring(_.value >= 0)
        m + (ByteBuffer.wrap(b.id) -> b)
    }
    SimpleState(newVersion, amap)
  }

  override type NVCT = SimpleState

  override def validate(transaction: BaseEvent): Try[Unit] = Try {

    //TODO implement me
  }

  override def changes(block: SimpleBlock): Try[BoxStateChanges[PublicKey25519Proposition, TodoBox]] =
    Try {
      val initial = (Seq(): Seq[Array[Byte]], Seq(): Seq[TodoBox])

      val (toRemove: Seq[ADKey @unchecked], toAdd: Seq[TodoBox]) =
        block.transactions.foldLeft(initial) {
          case ((sr, sa), tx) =>
            ((sr ++ tx.boxIdsToOpen.toSet).map(id => ADKey @@ id), sa ++ tx.newBoxes.toSet)
        }

      //for PoS forger reward box, we use block Id as a nonce
      val forgerNonce = Nonce @@ Longs.fromByteArray(block.id.take(8))
      val forgerBox = TodoBox(block.generator, forgerNonce,true)
      val ops: Seq[BoxStateChangeOperation[PublicKey25519Proposition, TodoBox]] =
        toAdd.map(b => Insertion[PublicKey25519Proposition, TodoBox](b)) ++
          Seq(Insertion[PublicKey25519Proposition, TodoBox](forgerBox))

      BoxStateChanges[PublicKey25519Proposition, TodoBox](ops)
    }

  override def semanticValidity(tx: BaseEvent): Try[Unit] = Success()

  override def validate(mod: SimpleBlock): Try[Unit] = Try(mod.transactions.foreach(tx => validate(tx).get))
}

object SimpleState {
  val EmptyVersion: VersionTag = VersionTag @@ Array.fill(32)(0: Byte)
}