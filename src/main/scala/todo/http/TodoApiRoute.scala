package todo.http

import akka.actor.{ActorRef, ActorRefFactory}
import akka.http.scaladsl.server.Route
import io.circe.syntax._
import scorex.core.api.http.{ApiRouteWithFullView, SuccessApiResponse}
import scorex.core.settings.RESTApiSettings
import scorex.crypto.encode.Base58
import todo.SimpleCommandMemPool
import todo.model.{SimpleBlockchain, SimpleState, SimpleWallet}

case class TodoApiRoute(override val settings: RESTApiSettings, nodeViewHolderRef: ActorRef)(implicit val context: ActorRefFactory)
  extends ApiRouteWithFullView[SimpleBlockchain, SimpleState, SimpleWallet, SimpleCommandMemPool] {

  override val route = (pathPrefix("todo") & withCors) {
    create ~ getAll
  }

  def create: Route = (get & path(IntNumber)) { count =>
    withNodeView { view =>
      val wallet = view.vault
      val boxes = wallet.boxes()
      val lastBlockIds = view.history.lastBlockIds(count)
      val tail = lastBlockIds.map(id => Base58.encode(id).asJson)
      complete(SuccessApiResponse("count" -> count.asJson, "tail" -> tail.asJson))
    }
  }

  def getAll: Route = (get & pathEndOrSingleSlash) {
    withNodeView { view =>
      val todoLists = view.state.storage.map {_._2}.filter(_.isForgerBox == false)
      complete(SuccessApiResponse(
        "count" -> todoLists.size.asJson,
        "todo" -> todoLists.map(_.json).asJson)
      )
    }
  }

}