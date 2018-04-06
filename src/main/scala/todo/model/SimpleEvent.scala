package todo.model

import com.google.common.primitives.{Bytes, Longs}
import io.circe.Json
import io.circe.syntax._
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

// modifier for Box
sealed trait BaseEvent extends BoxTransaction[PublicKey25519Proposition, SimpleBox] {
  val fee: Long
  val timestamp: Long
  val owner: PublicKey25519Proposition

  def nonceFromDigest(digest: Array[Byte]): Nonce = Nonce @@ Longs.fromByteArray(digest.take(8))
}

case class SimpleEvent(
                        owner: PublicKey25519Proposition,
                        signature: Signature25519
                      ) extends BaseEvent {

  override type M = SimpleEvent

  //no need to open anything during create action
  override lazy val unlockers: Traversable[BoxUnlocker[PublicKey25519Proposition]] = IndexedSeq()

  lazy val hashNoNonces = Blake2b256(
    Bytes.concat(
      //      scorex.core.utils.concatFixLengthBytes(to.map(_._1.pubKeyBytes)), //FIXME ?
      scorex.core.utils.concatFixLengthBytes(unlockers.map(_.closedBoxId)),
      Longs.toByteArray(timestamp),
      Longs.toByteArray(fee)
    )
  )

  override lazy val newBoxes: Traversable[SimpleBox] = Seq(
    SimpleBox(owner, nonceFromDigest(Blake2b256(owner.pubKeyBytes ++ hashNoNonces)))
  )

  override lazy val serializer = SimpleEventSerializer

  override lazy val json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    "timestamp" -> timestamp.asJson,
    "newBoxes" -> newBoxes.map(b => Base58.encode(b.id).asJson).toSeq.asJson,
  ).asJson

  override def toString: String = s"CreateSimpleOffer(${json.noSpaces})"

  override val fee: Long = 0
  override val timestamp: Long = 0
}

object SimpleEvent {

  def apply(trader: PublicKey25519Proposition, priv: PrivateKey25519): BaseEvent = {
    val fakeSig = Signature25519(Signature @@ Array[Byte]())
    val undersigned = SimpleEvent(trader, fakeSig)
    val msg = undersigned.messageToSign
    SimpleEvent(trader, PrivateKey25519Companion.sign(priv, msg))
  }

}

object SimpleEventSerializer extends Serializer[SimpleEvent] {

  override def toBytes(obj: SimpleEvent): Array[Byte] = Bytes.concat( /*FIXME*/  )

  override def parseBytes(bytes: Array[Byte]): Try[SimpleEvent] = ???
}
