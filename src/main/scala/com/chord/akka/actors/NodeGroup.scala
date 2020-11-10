package com.chord.akka.actors

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.chord.akka.actors.NodeGroup.Command
import com.chord.akka.utils.{Helper, SystemConstants}


object NodeGroup {

  var NodeList = new Array[ActorPath](SystemConstants.num_nodes)

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
        for (i <- 0 until n) {
          val nodeId: String =Helper.getIdentifier(Helper.generateRandomName()).toString
          val node = context.spawn(NodeActor(nodeId), nodeId)
          NodeList(i) = node.path
          context.log.info("Node Created " + node.path.toString)
        }

        this
    }
}





