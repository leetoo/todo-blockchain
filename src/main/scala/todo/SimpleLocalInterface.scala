package todo

import akka.actor.{ActorRef, ActorSystem, Props}
import todo.mining.{MiningSettings, SimpleForger}
import todo.model.{SimpleBlock, BaseEvent}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.{LocalInterface, ModifierId}

class SimpleLocalInterface(override val viewHolderRef: ActorRef,
                           forgerRef: ActorRef,
                           minerSettings: MiningSettings)
  extends LocalInterface[PublicKey25519Proposition, BaseEvent, SimpleBlock] {

  private var block = false

  override protected def onStartingPersistentModifierApplication(pmod: SimpleBlock): Unit = {}

  override protected def onFailedTransaction(tx: BaseEvent): Unit = {}

  override protected def onSyntacticallyFailedModification(mod: SimpleBlock): Unit = {}

  override protected def onSuccessfulTransaction(tx: BaseEvent): Unit = {}

  override protected def onSyntacticallySuccessfulModification(mod: SimpleBlock): Unit = {}

  override protected def onSemanticallyFailedModification(mod: SimpleBlock): Unit = {}

  override protected def onNewSurface(newSurface: Seq[ModifierId]): Unit = {}

  override protected def onRollbackFailed(): Unit = {
    log.error("Too deep rollback occurred!")
  }

  override protected def onSemanticallySuccessfulModification(mod: SimpleBlock): Unit = {}

  override protected def onNoBetterNeighbour(): Unit = forgerRef ! SimpleForger.StartMining

  override protected def onBetterNeighbourAppeared(): Unit = forgerRef ! SimpleForger.StopMining
}

object SimpleLocalInterfaceRef {
  def props(viewHolderRef: ActorRef,
            minerRef: ActorRef,
            minerSettings: MiningSettings): Props =
    Props(new SimpleLocalInterface(viewHolderRef, minerRef, minerSettings))

  def apply(viewHolderRef: ActorRef,
            minerRef: ActorRef,
            minerSettings: MiningSettings)
           (implicit system: ActorSystem): ActorRef =
    system.actorOf(props(viewHolderRef, minerRef, minerSettings))

  def apply(name: String, viewHolderRef: ActorRef,
            minerRef: ActorRef,
            minerSettings: MiningSettings)
           (implicit system: ActorSystem): ActorRef =
    system.actorOf(props(viewHolderRef, minerRef, minerSettings), name)
}
