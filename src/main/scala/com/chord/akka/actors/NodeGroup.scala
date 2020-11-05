package com.chord.akka.actors

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Signal}
import com.chord.akka.actors.NodeGroup.Command

/*
*
* Created by: prajw
* Date: 04-Nov-20
*
*/


object NodeGroup {
  def apply(): Behavior[Command] =
    Behaviors.setup(context => new NodeGroup(context))

  sealed trait Command

  final case class getValues(replyTo: ActorRef[LookupObjects]) extends Command

  final case class addValue(value: LookupObject, replyTo: ActorRef[ActionSuccessful]) extends Command

  final case class getValue(k: String, replyTo: ActorRef[GetLookupResponse]) extends Command


  case class ActionSuccessful(description: String)

  case class GetLookupResponse(maybeObject: Option[LookupObject])

}

class NodeGroup(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {

  import NodeGroup._

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case getValues(value) =>
        this
      case addValue(value, replyTo) =>
        this
      case getValue(k, replyTo) =>
        this
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = super.onSignal
}




