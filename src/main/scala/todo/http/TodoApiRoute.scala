package todo.http

import akka.actor.{ActorRef, ActorRefFactory}
import akka.http.scaladsl.server.Route
import io.circe.parser.parse
import io.circe.syntax._
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.api.http.{ApiException, ApiRouteWithFullView, SuccessApiResponse}
import scorex.core.settings.RESTApiSettings
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import todo.SimpleCommandMemPool
import todo.model.{CreateTodoEvent, SimpleBlockchain, SimpleState, SimpleWallet}

import scala.util.{Failure, Success, Try}

case class TodoApiRoute(override val settings: RESTApiSettings, nodeViewHolderRef: ActorRef)(implicit val context: ActorRefFactory)
  extends ApiRouteWithFullView[SimpleBlockchain, SimpleState, SimpleWallet, SimpleCommandMemPool] {

  override val route = (pathPrefix("todo") & withCors) {
    create ~ getAll ~ getOne
  }

  def getAll: Route = (get & pathEndOrSingleSlash) {
    withNodeView { view =>
      val todoLists = view.state.storage.map(_._2).filter(_.isForgerBox == false)
      complete(SuccessApiResponse(
        "count" -> todoLists.size.asJson,
        "todo" -> todoLists.map(_.json).asJson
      ))
    }
  }

  def getOne: Route = (get & path(Segment) ) { id =>
    withNodeView { view =>
      val todoLists = view.state.storage.map(_._2).filter(b => b.isForgerBox == false && b.id.toString == id).headOption
      complete(SuccessApiResponse(
        "todo" -> todoLists.map(_.json).asJson
      ))
    }
  }


  def create: Route = (post) {
    entity(as[String]) { body =>
      withNodeView { view =>
        parse(body) match {
          case Left(failure) => complete(ApiException(failure.getCause))
          case Right(json) => Try {
            val title: String = (json \\ "title").headOption.flatMap(_.asString).get
            val secret = view.vault.secrets.head
            val public = view.vault.publicKeys.head
            val createEvent = CreateTodoEvent(public, secret, title)
            nodeViewHolderRef ! LocallyGeneratedTransaction[PublicKey25519Proposition, CreateTodoEvent](createEvent)
            createEvent.json
          } match {
            case Success(resp) => complete(SuccessApiResponse(resp))
            case Failure(e) => complete(ApiException(e))
          }
        }
      }
    }
  }

}