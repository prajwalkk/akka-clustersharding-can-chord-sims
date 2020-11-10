package com.chord.akka.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.chord.akka.actors.NodeGroup.Command


object NodeGroup {

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new NodeGroup(context))

  sealed trait Command

  final case class createNodes(num_users: Int) extends Command

}

// Need to implement successor , fingerTable
class NodeGroup(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {

  import NodeGroup._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case createNodes(n) =>
        context.log.info(s"Creating $n Nodes")
        val nodeList = new Array[String](n)
        val createdNodes = for (i <- 0 until n) yield {
          val nodeId: String = "Node-" + i
          nodeList(i) = nodeId
          context.spawn(NodeActor(nodeId), nodeId)
        }
        createdNodes.foreach(node => context.log.info(s"NodeRef $node"))
        this
    }
}





