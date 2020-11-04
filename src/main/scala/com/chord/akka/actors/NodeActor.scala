package com.chord.akka.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.chord.akka.actors.NodeActor.Command
import com.typesafe.scalalogging.LazyLogging


object NodeActor  {

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new NodeActor(context))

  sealed trait Command
  final case class addNodesToChordRing(num_users: Int) extends Command
}

class NodeActor(context: ActorContext[Command]) extends AbstractBehavior[Command](context) with LazyLogging {
  import NodeActor._
  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case addNodesToChordRing(n) =>

        logger.info("Creating "+n+" Users")
        this


    }
}





