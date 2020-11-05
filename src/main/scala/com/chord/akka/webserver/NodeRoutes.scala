package com.chord.akka.webserver

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.chord.akka.actors.NodeGroup.{ActionSuccessful, GetLookupResponse}
import com.chord.akka.actors.{LookupObject, NodeGroup}

import scala.concurrent.Future

/*
*
* Created by: prajw
* Date: 05-Nov-20
*
*/
class NodeRoutes(nodeRegistry: ActorRef[NodeGroup.Command])(implicit val system: ActorSystem[_]){
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getValues(k: String): Future[GetLookupResponse] =
    nodeRegistry.ask(NodeGroup.getValue(k, _))
  def putValues(lookupObject: LookupObject): Future[ActionSuccessful] =
    nodeRegistry.ask(NodeGroup.addValue(lookupObject, _))


  // all routes
  // lookup - get
  // add - post

  // val nodeRoutes
}
