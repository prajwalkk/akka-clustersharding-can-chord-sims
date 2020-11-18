package com.chord.akka.actors

import java.time.LocalDateTime

import akka.actor.ActorPath
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.chord.akka.actors.NodeActorTest.{Join, NodeSetup, PrintUpdate, SaveNodeSnapshot}
import com.chord.akka.actors.NodeGroup.SaveAllSnapshot
import com.chord.akka.utils.{SystemConstants, YamlDumpFingerTableEntity, YamlDumpNodeProps}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer





object NodeGroup extends LazyLogging{

  case class NodeSnapshot(ts: LocalDateTime, nodeSetup: NodeSetup){
    def apply(ts: LocalDateTime, nodeSetup: NodeSetup): NodeSnapshot = new NodeSnapshot(ts, nodeSetup)
  }

  var NodeList = new Array[ActorPath](SystemConstants.num_nodes)
  var createdNodes = new ListBuffer[ActorRef[NodeActorTest.Command]]
  val nodeSnapshots = new ListBuffer[NodeSnapshot]()
  sealed trait Command
  final case class CreateNodes(num_users: Int) extends Command
  final case object SaveSnapshot extends Command
  final case object SaveAllSnapshot extends Command
  case class ReplySnapshot(nodeSnapshot: NodeSnapshot) extends Command

  def apply(): Behavior[Command] =
    nodeGroupOperations()


  def nodeGroupOperations(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>

      message match {

        case CreateNodes(SystemConstants.num_nodes) => {
          context.log.info(s"Creating $SystemConstants.num_nodes Nodes")
          val nodeList = new Array[ActorRef[NodeActorTest.Command]](SystemConstants.num_nodes)
           for (i <- 0 until SystemConstants.num_nodes) yield {
            val nodeName: String = s"Node_$i"
            val actorRef = context.spawn(NodeActorTest(nodeName = nodeName), nodeName)
            nodeList(i) = actorRef
            NodeList(i) = actorRef.path
            if (i == 0) {
              actorRef ! Join(actorRef)
              Thread.sleep(1000)
            }
            else {
              actorRef ! Join(nodeList(0))
              Thread.sleep(1000)
            }
          createdNodes +=  actorRef
          }
          Thread.sleep(30000)
          //createdNodes.foreach(i => i ! PrintUpdate)
        //  createdNodes.foreach(i => i ! SaveNodeSnapshot(context.self))
          //createdNodes.foreach(node => context.log.info(s"Created Nodes are: NodeRef ${node.path.name}"))
          Behaviors.same
        }

        case SaveSnapshot => {
          context.log.debug("asking for snapshot")
          context.children.foreach { child =>
            child.asInstanceOf[ActorRef[NodeActorTest.Command]] ! SaveNodeSnapshot(context.self)
          }
          Behaviors.same
        }
        case SaveAllSnapshot => {
          createdNodes.toList.foreach(actorRef => actorRef ! SaveNodeSnapshot(context.self))
          Behaviors.same
        }
        case ReplySnapshot(nodeSnapshot) => {
          context.log.debug("got snapshot")
          writeYaml(nodeSnapshot)
          Behaviors.same
        }
      }

    }


  }

  def writeYaml(nodeSnapshots: NodeSnapshot): Unit ={
    import net.jcazevedo.moultingyaml._
    import com.chord.akka.utils.MyYamlProtocol._
    val nodeSetup = nodeSnapshots.nodeSetup
    val ts = nodeSnapshots.ts.toString
    val snapShotClass = YamlDumpNodeProps(ts, nodeSetup)
    val yaml = snapShotClass.toYaml
    logger.info(yaml.prettyPrint)

  }



}








