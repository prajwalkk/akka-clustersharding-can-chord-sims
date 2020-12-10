package com.can.akka.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.can.akka.actors.NodeActor.CAN_Join
import com.can.akka.simulation.Simulation.{sharding, typekey}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object NodeGroup extends LazyLogging {

  var NodeList = new ArrayBuffer[String]()
  var nodelist = new ArrayBuffer[EntityRef[NodeActor.Command]]

  def apply(): Behavior[Command] =
    nodeGroupBehavior()

  private def nodeGroupBehavior(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case CreateNode(replyTo, i) => {
          implicit val timeout: Timeout = Timeout(20.seconds)
          implicit val scheduler: Scheduler = context.system.scheduler
          val node_name = s"node_$i"
          val node: EntityRef[NodeActor.Command] = sharding.entityRefFor(typekey, node_name)
          if (i == 0) {
            val future = node.ask[NodeActor.ActionSuccessful](ref => CAN_Join(node, ref))
            val status = Await.result(future, timeout.duration).description
            context.log.info(s"[CreateNode] $status")
            NodeList += node_name
            nodelist += node
          }
          else {
            val future = node.ask[NodeActor.ActionSuccessful](ref => CAN_Join(nodelist(0), ref))
            val status = Await.result(future, timeout.duration).description
            context.log.debug(s"[CreateNode] $status")
            NodeList += node_name
            nodelist += node
          }
          replyTo ! ActionSuccessful(s"Node Created")
          Behaviors.same
        }
      }
    }
  }

  sealed trait Command

  final case class CreateNode(replyTo: ActorRef[ActionSuccessful], i: Int) extends Command

  case class ActionSuccessful(description: String) extends Command
}