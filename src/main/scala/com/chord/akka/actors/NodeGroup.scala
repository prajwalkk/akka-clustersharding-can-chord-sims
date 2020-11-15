package com.chord.akka.actors

import akka.actor.ActorPath
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.chord.akka.actors.NodeActorTest.Join
import com.chord.akka.utils.SystemConstants


object NodeGroup {
  var NodeList = new Array[ActorPath](SystemConstants.num_nodes)
  sealed trait Command
  final case class createNodes(num_users: Int) extends Command


  def apply(): Behavior[Command] =
    nodeGroupOperations()


  def nodeGroupOperations(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case createNodes(num_users) =>
          context.log.info(s"Creating $num_users Nodes")
          val nodeList = new Array[ActorRef[NodeActorTest.Command]](num_users)
          val createdNodes = for (i <- 0 until num_users) yield {
            val nodeName: String = s"Node_$i"
            val actorRef = context.spawn(NodeActorTest(nodeName = nodeName), nodeName)
            nodeList(i) = actorRef
            NodeList(i)= actorRef.path
            if (i == 0) {
              actorRef ! Join(actorRef)
              Thread.sleep(1000)
            }
            else {
              actorRef ! Join(nodeList(0))
              Thread.sleep(1000)
            }
            actorRef
          }
          createdNodes.foreach(node => context.log.info(s"Created Nodes are: NodeRef ${node.path.name}"))
          Behaviors.same
      }

    }
  }

}








