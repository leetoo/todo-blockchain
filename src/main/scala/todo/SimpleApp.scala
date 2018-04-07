package todo

import akka.actor.{ActorRef, Props}
import scorex.core.api.http.{ApiRoute, NodeViewApiRoute}
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizerRef
import scorex.core.network.message.MessageSpec
import scorex.core.settings.ScorexSettings
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import todo.http.{DebugApiRoute, StatsApiRoute}
import todo.mining.{AppSettings, SimpleForger}
import todo.model._

import scala.io.Source
import scala.language.postfixOps

class SimpleApp(val settingsFilename: String) extends Application {

  override type P = PublicKey25519Proposition
  override type TX = BaseEvent
  override type PMOD = SimpleBlock
  override type NVHT = SimpleNodeViewHolder

  private val hybridSettings = AppSettings.read(Some(settingsFilename))

  implicit override lazy val settings: ScorexSettings = AppSettings.read(Some(settingsFilename)).scorexSettings

  log.debug(s"Starting application with settings \n$settings")

  override protected lazy val additionalMessageSpecs: Seq[MessageSpec[_]] = Seq(SimpleSyncInfoMessageSpec)

  override val nodeViewHolderRef: ActorRef = SimpleNodeViewHolderRef(settings, hybridSettings.mining, timeProvider)

  override val apiRoutes: Seq[ApiRoute] = Seq(
    StatsApiRoute(settings.restApi, nodeViewHolderRef),
    DebugApiRoute(settings.restApi, nodeViewHolderRef),
    NodeViewApiRoute[P, TX](settings.restApi, nodeViewHolderRef),
  )

  override val swaggerConfig: String = Source.fromResource("api/testApi.yaml").getLines.mkString("\n")

  val forger: ActorRef = actorSystem.actorOf(Props(new SimpleForger(nodeViewHolderRef, hybridSettings.mining, timeProvider)))

  override val localInterface: ActorRef = SimpleLocalInterfaceRef(nodeViewHolderRef, forger, hybridSettings.mining)

  override val nodeViewSynchronizer: ActorRef =
    actorSystem.actorOf(NodeViewSynchronizerRef.props[P, TX, SimpleSyncInfo, SimpleSyncInfoMessageSpec.type, PMOD, SimpleBlockchain, SimpleCommandMemPool](networkControllerRef, nodeViewHolderRef, localInterface,
      SimpleSyncInfoMessageSpec, settings.network, timeProvider))

}

object SimpleApp extends App {
  val settingsFilename = args.headOption.getOrElse("settings.conf")
  new SimpleApp(settingsFilename).run()
}
