package com.chord.akka.actors

import akka.actor.typed.ActorRef

/*
*
* Created by: prajw
* Date: 04-Nov-20
*
*/



object NodeGroup {

  trait Command
  final case class addValue(value: LookupObject, replyTo: ActorRef[ActionSuccessful]) extends Command
  final case class getValue(k: String, replyTo: ActorRef[GetLookupResponse]) extends Command


  case class ActionSuccessful(description: String)

  case class GetLookupResponse(maybeUser: Option[LookupObject])

}


