package exchange

import exchange.model.BaseEvent
import io.iohk.iodb.ByteArrayWrapper
import scorex.core.ModifierId
import scorex.core.transaction.MemoryPool
import scorex.core.utils.ScorexLogging

import scala.collection.concurrent.TrieMap
import scala.util.{Success, Try}

case class SimpleCommandMemPool(unconfirmed: TrieMap[ByteArrayWrapper, BaseEvent])
  extends MemoryPool[BaseEvent, SimpleCommandMemPool] with ScorexLogging {

  override type NVCT = SimpleCommandMemPool

  private def key(id: Array[Byte]): ByteArrayWrapper = ByteArrayWrapper(id)

  //getters
  override def getById(id: ModifierId): Option[BaseEvent] =
    unconfirmed.get(key(id))

  override def contains(id: ModifierId): Boolean = unconfirmed.contains(key(id))

  override def getAll(ids: Seq[ModifierId]): Seq[BaseEvent] = ids.flatMap(getById)

  //modifiers
  override def put(tx: BaseEvent): Try[SimpleCommandMemPool] = Success {
    unconfirmed.put(key(tx.id), tx)
    this
  }

  //todo
  override def put(txs: Iterable[BaseEvent]): Try[SimpleCommandMemPool] = Success(putWithoutCheck(txs))

  override def putWithoutCheck(txs: Iterable[BaseEvent]): SimpleCommandMemPool = {
    txs.foreach(tx => unconfirmed.put(key(tx.id), tx))
    this
  }

  override def remove(tx: BaseEvent): SimpleCommandMemPool = {
    unconfirmed.remove(key(tx.id))
    this
  }

  override def take(limit: Int): Iterable[BaseEvent] =
    unconfirmed.values.toSeq.sortBy(-_.fee).take(limit)

  override def filter(condition: (BaseEvent) => Boolean): SimpleCommandMemPool = {
    unconfirmed.retain { (k, v) =>
      condition(v)
    }
    this
  }

  override def size: Int = unconfirmed.size
}

object SimpleCommandMemPool {
  lazy val emptyPool: SimpleCommandMemPool = SimpleCommandMemPool(TrieMap())
}