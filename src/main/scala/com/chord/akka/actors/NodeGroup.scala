package com.chord.akka.actors

import java.nio.file.{OpenOption, Paths, StandardOpenOption}
import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.ActorPath
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, _}
import akka.util.ByteString
import com.chord.akka.actors.NodeActor.{Join, NodeSetup, SaveNodeSnapshot}
import com.chord.akka.utils.{SystemConstants, YamlDumpMainHolder, YamlDumpNodeProps}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future





object NodeGroup extends LazyLogging{

  case class NodeSnapshot(ts: LocalDateTime, nodeSetup: NodeSetup){
    def apply(ts: LocalDateTime, nodeSetup: NodeSetup): NodeSnapshot = new NodeSnapshot(ts, nodeSetup)
  }

  var NodeList = new Array[ActorPath](SystemConstants.num_nodes)
  var createdNodes = new ListBuffer[ActorRef[NodeActor.Command]]
  val nodeSnapshots = new ListBuffer[NodeSnapshot]()
  sealed trait Command
  final case class CreateNodes(num_users: Int) extends Command
  final case object SaveSnapshot extends Command
  case class ReplySnapshot(nodeSnapshot: NodeSnapshot) extends Command
  case class ReplyWithJoinStatus(str: String) extends Command
  case class SaveAllSnapshot() extends Command


  def apply(): Behavior[Command] =
    nodeGroupOperations()
  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  def writeYaml(nodeSnapshots: NodeSnapshot, context: ActorContext[Command]): Unit ={
    import com.chord.akka.utils.MyYamlProtocol._
    import net.jcazevedo.moultingyaml._


    implicit val system: ActorSystem[Nothing] = context.system
    val nodeSetup = nodeSnapshots.nodeSetup
    val ts = nodeSnapshots.ts.toString
    val snapShotClass = YamlDumpNodeProps(ts, nodeSetup)
    val mainYamlClass = YamlDumpMainHolder(LocalDateTime.now().toString, snapShotClass)
    val yaml = mainYamlClass.toYaml

    val yamlSource:Source[String, NotUsed] =  Source.single(yaml.prettyPrint)
    val fileName = s"yamldump_${LocalDateTime.now().toLocalDate.toString}.yaml"
    val fileOpenOptions: Set[OpenOption] = Set(StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)
    val yamlResult: Future[IOResult] =
      yamlSource.map(char => ByteString(char))
        .runWith(FileIO.toPath(Paths.get(fileName), fileOpenOptions))
    //logger.info(yaml.prettyPrint)
    context.log.debug("YAML done")
  }

  def nodeGroupOperations(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>

      message match {

        case CreateNodes(SystemConstants.num_nodes) =>
          context.log.info(s"Creating $SystemConstants.num_nodes Nodes")
          val nodeList = new Array[ActorRef[NodeActor.Command]](SystemConstants.num_nodes)
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

          createdNodes.foreach(node => context.log.info(s"Created Nodes are: NodeRef ${node.path.name}"))
          Behaviors.same
        case SaveSnapshot(actorRef) => {
          actorRef ! SaveNodeSnapshot(context.self)

          Behaviors.same
        }

        case ReplySnapshot(nodeSnapshot) => {
          context.log.debug("got snapshot")
          writeYaml(nodeSnapshot)
          Behaviors.same
        }
        case SaveAllSnapshot() =>
          createdNodes.toList.foreach(actorRef => actorRef ! SaveNodeSnapshot(context.self))
          Behaviors.same

      }

    }


  }



}








