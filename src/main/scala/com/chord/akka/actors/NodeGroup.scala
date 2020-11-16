package com.chord.akka.actors

import java.time.LocalDateTime

import akka.actor.ActorPath
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import com.chord.akka.actors.NodeActorTest.{Join, NodeSetup, PrintUpdate, SaveNodeSnapshot}
import com.chord.akka.utils.{SystemConstants, YamlDumpFingerTableEntity, YamlDumpNodeProps}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}





object NodeGroup extends LazyLogging{

  case class NodeSnapshot(ts: LocalDateTime, nodeSetup: NodeSetup){
    def apply(ts: LocalDateTime, nodeSetup: NodeSetup): NodeSnapshot = new NodeSnapshot(ts, nodeSetup)
  }

  var NodeList = new Array[ActorPath](SystemConstants.num_nodes)

  val nodeSnapshots = new ListBuffer[NodeSnapshot]()
  sealed trait Command
  final case class CreateNodes(num_users: Int) extends Command
  final case class SaveSnapshot(actorRef: ActorRef[NodeActorTest.SaveNodeSnapshot]) extends Command
  case class ReplySnapshot(nodeSnapshot: NodeSnapshot) extends Command
  case class ReplyWithJoinStatus(str: String) extends Command

  def apply(): Behavior[Command] =
    nodeGroupOperations()


  def nodeGroupOperations(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>

      message match {

        case CreateNodes(num_users) => {
          context.log.info(s"Creating $num_users Nodes")
          val nodeList = new Array[ActorRef[NodeActorTest.Command]](num_users)
          val createdNodes = for (i <- 0 until num_users) yield {
            val nodeName: String = s"Node_$i"
            val actorRef = context.spawn(NodeActorTest(nodeName = nodeName), nodeName)
            nodeList(i) = actorRef
            NodeList(i) = actorRef.path
            implicit val timeout: Timeout = Timeout(100.seconds)
            if (i == 0) {
              context.ask(actorRef, res => Join(nodeList(0), res)){
                case Success(ReplyWithJoinStatus("joinSuccess")) => SaveSnapshot(actorRef)
              }
              Thread.sleep(10000)
            }
            else {
              context.ask(actorRef, res => Join(nodeList(0), res)){
                case Success(ReplyWithJoinStatus("joinSuccess")) => SaveSnapshot(actorRef)
              }
              Thread.sleep(10000)
            }
            actorRef
          }
          Thread.sleep(30000)
          //createdNodes.foreach(i => i ! PrintUpdate)
          //createdNodes.foreach(i => i ! SaveNodeSnapshot(context.self))
          //createdNodes.foreach(node => context.log.info(s"Created Nodes are: NodeRef ${node.path.name}"))
          Behaviors.same
        }

        case SaveSnapshot(actorRef) => {
            actorRef ! SaveNodeSnapshot(context.self)

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








