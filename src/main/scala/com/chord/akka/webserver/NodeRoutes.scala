package com.chord.akka.webserver

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.chord.akka.actors.NodeGroup.{ActionSuccessful, GetLookupResponse}
import com.chord.akka.actors.{LookupObject, LookupObjects, NodeGroup}

import scala.concurrent.Future

/*
*
* Created by: prajw
* Date: 05-Nov-20
*
*/
class NodeRoutes(nodeRegistry: ActorRef[NodeGroup.Command])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))
  val lookupRoutes: Route =
    pathPrefix("chord") {
      concat(
        //case: lookup/
        pathEnd {
          // 2 cases: GET and POST
          concat(
            get {
              complete(getValues())
            },
            post {
              entity(as[LookupObject]) { lookupObject =>
                onSuccess(putValues(lookupObject)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { k =>
          concat(
            get {
              // retrieve single lookup info
              rejectEmptyResponse {
                onSuccess(getValue(k)) { response =>
                  complete(response.maybeObject)
                }
              }
            })
        })
    }


  def getValues(): Future[LookupObjects] =
    nodeRegistry.ask(NodeGroup.getValues)

  def getValue(k: String): Future[GetLookupResponse] =
    nodeRegistry.ask(NodeGroup.getValue(k, _))


  // all routes
  // lookup - get
  // add - post

  def putValues(lookupObject: LookupObject): Future[ActionSuccessful] =
    nodeRegistry.ask(NodeGroup.addValue(lookupObject, _))
}
