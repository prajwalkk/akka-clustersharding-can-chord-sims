package com.chord.akka.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.chord.akka.actors.NodeActor.Command
import com.typesafe.scalalogging.LazyLogging


object NodeActor  {

  def apply(nodeId:String): Behavior[Command] =
    Behaviors.setup(context => new NodeActor(context,nodeId))

  sealed trait Command
  final case class addNodesToChordRing(num_users: Int) extends Command
}
// Need to implement successor , fingerTable
class NodeActor(context: ActorContext[Command],chordNodeId:String) extends AbstractBehavior[Command](context) with LazyLogging {
  import NodeActor._
  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case addNodesToChordRing(n) =>
        logger.info("Creating "+n+" Users")
        val nodeList = new Array[String](n)
        for(i<- 0 until n){
          val nodeId: String = "Node-"+i
          nodeList(i) = nodeId
          context.spawn(NodeActor(nodeId),nodeId)
        }
        this


    }
}





