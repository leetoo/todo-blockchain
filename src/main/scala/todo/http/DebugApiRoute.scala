package todo.http

import akka.actor.{ActorRef, ActorRefFactory}
import akka.http.scaladsl.server.Route
import io.circe.syntax._
import scorex.core.api.http.{ApiRouteWithFullView, SuccessApiResponse}
import scorex.core.settings.RESTApiSettings
import scorex.crypto.encode.Base58
import todo.SimpleCommandMemPool
import todo.model.{SimpleBlockchain, SimpleState, SimpleWallet}

case class DebugApiRoute(override val settings: RESTApiSettings, nodeViewHolderRef: ActorRef)(implicit val context: ActorRefFactory)
  extends ApiRouteWithFullView[SimpleBlockchain, SimpleState, SimpleWallet, SimpleCommandMemPool] {

  override val route = (pathPrefix("debug") & withCors) {
    chain
  }

  def chain: Route = (get & path("chain")) {
    withNodeView { view =>
      complete(SuccessApiResponse("history" -> view.history.blocks.toSeq.sortBy(- _._2.timestamp).map(_._2.json).asJson))
    }
  }


}