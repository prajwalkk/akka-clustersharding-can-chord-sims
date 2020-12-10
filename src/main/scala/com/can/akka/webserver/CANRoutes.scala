package com.can.akka.webserver

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.can.akka.actors.NodeActor.{ActionSuccessful, FindNode, SearchDataNode}
import com.can.akka.actors.{NodeActor, RequestObject}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

class CANRoutes(Registry: EntityRef[NodeActor.Command])(implicit val system: ActorSystem[_]) extends LazyLogging {


  import CANJsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))
  val lookupRoutes: Route =
    pathPrefix("can") {
      concat(
        //case: lookup/
        pathEnd {
          // 2 cases: GET and POST
          concat(
            get {

              complete(getValues())
            },
            post {
              entity(as[RequestObject]) { requestObject =>
                onSuccess(putValues(requestObject)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { k =>
          concat(
            get {
              // retrieve single lookup info
              onSuccess(getValue(k)) { response =>
                complete(response.description)
              }
            })
        })
    }


  def getValues(): Future[ActionSuccessful] = {

    Registry.ask(NodeActor.getNodeStatus(_))
  }


  // all routes
  // lookup - get
  // add - post

  def putValues(requestObject: RequestObject): Future[ActionSuccessful] = {

    Registry.ask(ref => FindNode(requestObject, ref))
  }

  def getValue(k: String): Future[ActionSuccessful] = {

    Registry.ask(SearchDataNode(k, _))
  }


}
