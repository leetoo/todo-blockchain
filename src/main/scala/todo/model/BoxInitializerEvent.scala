package todo.model

import com.google.common.primitives.{Bytes, Ints, Longs}
import io.circe.Json
import io.circe.syntax._
import scorex.core.{ModifierId, ModifierTypeId}
import scorex.core.serialization.Serializer
import scorex.core.transaction.BoxTransaction
import scorex.core.transaction.box.BoxUnlocker
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.{PrivateKey25519, PrivateKey25519Companion}
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.Signature

import scala.util.Try

// base modifier for Box
sealed trait BaseEvent extends BoxTransaction[PublicKey25519Proposition, TodoBox] {
  val fee: Long
  val timestamp: Long
  val owner: PublicKey25519Proposition


  def nonceFromDigest(digest: Array[Byte]): Nonce = Nonce @@ Longs.fromByteArray(digest.take(8))
  def boxIdsToOpen: IndexedSeq[ModifierId]
}

case class BoxInitializerEvent(
                                owner: PublicKey25519Proposition,
                                signature: Signature25519
                              ) extends BaseEvent {

  override type M = BoxInitializerEvent

  override lazy val unlockers: Traversable[BoxUnlocker[PublicKey25519Proposition]] = IndexedSeq()

  override lazy val newBoxes: Traversable[TodoBox] = Seq()

  override def boxIdsToOpen: IndexedSeq[ModifierId] = IndexedSeq()

  override lazy val serializer = BoxInitializerEventSerializer

  override lazy val json: Json = Map(
  "id" -> Base58.encode(id).asJson,
  "timestamp" -> timestamp.asJson,
  "newBoxes" -> newBoxes.map(b => Base58.encode(b.id).asJson).toSeq.asJson,
  ).asJson

  override def toString: String = s"BoxInitializerEvent(${json.noSpaces})"
  override val fee: Long = 0

  override val timestamp: Long = 0
}

object BoxInitializerEvent {

  val ModifierTypeId: ModifierTypeId = scorex.core.ModifierTypeId @@ 2.toByte


  def apply(trader: PublicKey25519Proposition, priv: PrivateKey25519): BaseEvent = {
    val fakeSig = Signature25519(Signature @@ Array[Byte]())
    val undersigned = BoxInitializerEvent(trader, fakeSig)
    val msg = undersigned.messageToSign
    BoxInitializerEvent(trader, PrivateKey25519Companion.sign(priv, msg))
  }

}

object BoxInitializerEventSerializer extends Serializer[BoxInitializerEvent] {

  override def toBytes(obj: BoxInitializerEvent): Array[Byte] = Bytes.concat(/*FIXME*/)

  override def parseBytes(bytes: Array[Byte]): Try[BoxInitializerEvent] = ???
}


case class CreateTodoEvent(
                            owner: PublicKey25519Proposition,
                            signature: Signature25519,
                            title: String
                          ) extends BaseEvent {

  override type M = CreateTodoEvent

  override lazy val unlockers: Traversable[BoxUnlocker[PublicKey25519Proposition]] = IndexedSeq()

  override lazy val newBoxes: Traversable[TodoBox] = Seq(
    TodoBox(owner, nonceFromDigest(Blake2b256(owner.pubKeyBytes ++ title.map(_.toByte))), false, title)
  )

  override def boxIdsToOpen: IndexedSeq[ModifierId] = IndexedSeq()

  override lazy val serializer = CreateTodoEventSerializer

  override lazy val json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    "timestamp" -> timestamp.asJson,
    "newBoxes" -> newBoxes.map(b => Base58.encode(b.id).asJson).toSeq.asJson,
  ).asJson

  override def toString: String = s"CreateTodoEvent(${json.noSpaces})"

  override val fee: Long = 0
  override val timestamp: Long = 0
}


object CreateTodoEvent {

  val ModifierTypeId: ModifierTypeId = scorex.core.ModifierTypeId @@ 3.toByte

  def apply(trader: PublicKey25519Proposition, priv: PrivateKey25519, title: String): CreateTodoEvent = {
    val fakeSig = Signature25519(Signature @@ Array[Byte]())
    val undersigned = CreateTodoEvent(trader, fakeSig, title)
    val msg = undersigned.messageToSign
    CreateTodoEvent(trader, PrivateKey25519Companion.sign(priv, msg), title)
  }

}

object CreateTodoEventSerializer extends Serializer[CreateTodoEvent] {

  override def toBytes(obj: CreateTodoEvent): Array[Byte] = Bytes.concat(/*FIXME*/)

  override def parseBytes(bytes: Array[Byte]): Try[CreateTodoEvent] = ???
}




