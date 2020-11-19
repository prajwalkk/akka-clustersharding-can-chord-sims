package com.chord.akka.webserver

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.chord.akka.actors.NodeActor.ActionSuccessful
import com.chord.akka.actors.{NodeActor, RequestObject}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

/*
*
* Created by: prajw
* Date: 05-Nov-20
*
*/
class NodeRoutes(nodeRegistry: ActorRef[NodeActor.Command])(implicit val system: ActorSystem[_]) extends LazyLogging{

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

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


  def getValues(): Future[String] =
    nodeRegistry.ask(NodeActor.getValues)








  // all routes
  // lookup - get
  // add - post

  def putValues(requestObject: RequestObject): Future[ActionSuccessful] = {

    nodeRegistry.ask(NodeActor.FindNode(requestObject, _))
  }
  def getValue(k: String): Future[ActionSuccessful] = {

  nodeRegistry.ask(NodeActor.SearchDataNode(k, _))}



}
