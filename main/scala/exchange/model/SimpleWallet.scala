package exchange.model

import scorex.core.transaction.box.proposition.Constants25519.PrivKeyLength
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.PrivateKey25519
import scorex.core.transaction.wallet.{Wallet, WalletBox, WalletTransaction}
import scorex.core.utils.ByteStr
import scorex.core.{ModifierId, VersionTag}
import scorex.crypto.signatures.Curve25519
import scorex.utils.Random

import scala.util.Try

case class SimpleWallet(
  seed: ByteStr = ByteStr(Random.randomBytes(PrivKeyLength)),
  chainTransactions: Map[ModifierId, BaseEvent] = Map(),
  offchainTransactions: Map[ModifierId, BaseEvent] = Map(),
  currentBalance: Long = 0
)
  extends Wallet[PublicKey25519Proposition, BaseEvent, SimpleBlock, SimpleWallet] {
  override type S = PrivateKey25519
  override type PI = PublicKey25519Proposition

  override type NVCT = SimpleWallet

  //it's being recreated from seed on each wallet update, not efficient at all
  private val secret: S = {
    val pair = Curve25519.createKeyPair(seed.arr)
    PrivateKey25519(pair._1, pair._2)
  }

  private val pubKeyBytes = secret.publicKeyBytes

  override def secretByPublicImage(publicImage: PI): Option[S] = {
    if (publicImage.address == secret.publicImage.address) Some(secret) else None
  }

  override def generateNewSecret(): SimpleWallet = throw new Error("Only one secret is supported")

  override def secrets: Set[S] = Set(secret)

  override def rollback(to: VersionTag): Try[SimpleWallet] = ???

  override def publicKeys: Set[PI] = Set(secret.publicImage)

  override def historyTransactions: Seq[WalletTransaction[PublicKey25519Proposition, BaseEvent]] = ???

  override def boxes(): Seq[WalletBox[PublicKey25519Proposition, SimpleBox]] = Seq()

  override def scanOffchain(tx: BaseEvent): SimpleWallet = ???

  override def scanOffchain(txs: Seq[BaseEvent]): SimpleWallet =
    txs.foldLeft(this) { case (wallet, tx) => wallet.scanOffchain(tx) }

  override def scanPersistent(modifier: SimpleBlock): SimpleWallet = {
    modifier.transactions.foldLeft(this) {
      case (w, tx) =>
        tx match {
          case _ =>
            SimpleWallet(seed)
        }
    }
  }
}