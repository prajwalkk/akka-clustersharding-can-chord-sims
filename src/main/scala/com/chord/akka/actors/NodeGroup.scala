package com.chord.akka.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.chord.akka.actors.NodeActor.Join


object NodeGroup {

  sealed trait Command
  final case class createNodes(num_users: Int) extends Command


  def apply(): Behavior[Command] =
    nodeGroupOperations()


  def nodeGroupOperations(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case createNodes(num_users) =>
          context.log.info(s"Creating $num_users Nodes")
          val nodeList = new Array[ActorRef[NodeActor.Command]](num_users)
          val createdNodes = for (i <- 0 until num_users) yield {

            val nodeName: String = s"Node_$i"

            val actorRef = context.spawn(NodeActor(nodeName = nodeName), nodeName)
            nodeList(i) = actorRef
            if (i == 0) {
              actorRef ! Join(actorRef)
            }
            else {
              actorRef ! Join(nodeList(0))
            }
          }
          createdNodes.foreach(node => context.log.info(s"NodeRef $node"))
          Behaviors.same
      }

    }
  }

}








