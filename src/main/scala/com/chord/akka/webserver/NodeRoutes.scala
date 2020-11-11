package com.chord.akka.webserver

import akka.actor.ActorPath
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.chord.akka.actors.NodeActor.{ActionSuccessful, GetLookupResponse}
import com.chord.akka.actors.{LookupObject, LookupObjects, NodeActor, NodeGroup}
import com.chord.akka.utils.Helper
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer
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

  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))
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
              onSuccess(getValue(k)) { response =>
                complete(response.maybeObject)
              }
            })
        })
    }


  def getValues(): Future[LookupObjects] =
    nodeRegistry.ask(NodeActor.getValues)

  def getValue(k: String): Future[GetLookupResponse] =
    nodeRegistry.ask(NodeActor.getValue(k, _))


  // all routes
  // lookup - get
  // add - post

  def putValues(lookupObject: LookupObject): Future[ActionSuccessful] = {
    val node = findNode(lookupObject)
    logger.info(s"${lookupObject.key} will be stored at ${node}")
    nodeRegistry.ask(NodeActor.addValue(lookupObject, _))
  }

  def findNode(lookupObject: LookupObject): ActorPath ={
    val key = Helper.getIdentifier(lookupObject.key)

    val nodes  = new ListBuffer[ActorPath]
    for(path <- NodeGroup.NodeList.sorted){
      val nodekey =Helper.getIdentifier(path.toString.split("/").toSeq.last)

      if ( nodekey >= key) nodes += path
    }

    if(nodes.nonEmpty) nodes.head
    else NodeGroup.NodeList.sorted.toSeq(0)
  }
}
