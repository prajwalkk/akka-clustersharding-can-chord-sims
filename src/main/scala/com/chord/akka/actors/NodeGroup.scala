package com.chord.akka.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

/*
*
* Created by: prajw
* Date: 04-Nov-20
*
*/


object NodeGroup {

  // Requests
  sealed trait Command
  final case class getValues(replyTo: ActorRef[LookupObjects]) extends Command
  final case class addValue(lookupObject: LookupObject, replyTo: ActorRef[ActionSuccessful]) extends Command
  final case class getValue(k: String, replyTo: ActorRef[GetLookupResponse]) extends Command

  //Responses
  case class ActionSuccessful(description: String)
  case class GetLookupResponse(maybeObject: Option[LookupObject])

  def apply(): Behavior[Command] =
    nodeBehaviors(Set.empty)

  def nodeBehaviors(lookupObjectSet: Set[LookupObject]): Behavior[Command] = {
    Behaviors.receiveMessage {

      case getValues(replyTo: ActorRef[LookupObjects]) =>
        replyTo ! LookupObjects(lookupObjectSet.toSeq)
        Behaviors.same

      case addValue(lookupObject, replyTo) =>
        replyTo ! ActionSuccessful(s"object ${lookupObject.key} created")
        nodeBehaviors(lookupObjectSet + lookupObject)

      case getValue(k, replyTo) =>
        replyTo ! GetLookupResponse(lookupObjectSet.find(_.key == k))
        Behaviors.same
    }
  }


}




