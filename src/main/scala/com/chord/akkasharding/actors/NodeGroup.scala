package com.chord.akkasharding.actors

import java.nio.file.{OpenOption, Paths, StandardOpenOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.NotUsed
import akka.actor.ActorPath
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef, EntityTypeKey}
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, _}
import akka.util.ByteString
import com.chord.akkasharding.actors.NodeActor.{Join, NodeSetup, SaveNodeDataSnapshot, SaveNodeSnapshot}
import com.chord.akkasharding.utils.{SystemConstants, YamlDumpDataHolder, YamlDumpMainHolder, YamlDumpNodeProps}
import com.chord.akkasharding.utils.SystemConstants
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future





object NodeGroup extends LazyLogging{

  case class NodeSnapshot(ts: LocalDateTime, nodeSetup: NodeSetup){
    def apply(ts: LocalDateTime, nodeSetup: NodeSetup): NodeSnapshot = new NodeSnapshot(ts, nodeSetup)
  }

  //var NodeList = new Array[ActorPath](SystemConstants.num_nodes)
  var NodeList = new Array[String](SystemConstants.num_nodes)
  //added EntityRef instead of ActorRef
  var createdNodes = new ListBuffer[EntityRef[NodeActor.Command]]
  val nodeSnapshots = new ListBuffer[NodeSnapshot]()
  val fileOpenOptions: Set[OpenOption] = Set(StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)
  sealed trait Command
  //added
  final case class CreateNodes(num_nodes: Int,sharding: ClusterSharding,TypeKey:EntityTypeKey[NodeActor.Command]) extends Command
  final case class SaveSnapshot(actorRef: ActorRef[NodeActor.SaveNodeSnapshot]) extends Command
  case class ReplySnapshot(nodeSnapshot: NodeSnapshot) extends Command
  case class ReplyDataSnapshot(nodeSnapshot: NodeSnapshot) extends Command
  case class ReplyWithJoinStatus(str: String) extends Command
  case object SaveAllSnapshot extends Command
  case object SaveDataSnapshot extends Command


  def apply(): Behavior[Command] =
    nodeGroupOperations()
  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  def writeYaml(nodeSnapshots: NodeSnapshot, context: ActorContext[Command]): Unit = {
    import com.chord.akkasharding.utils.MyYamlProtocol._
    import net.jcazevedo.moultingyaml._


    implicit val system: ActorSystem[Nothing] = context.system
    val nodeSetup = nodeSnapshots.nodeSetup
    val ts = nodeSnapshots.ts.toString
    val snapShotClass = YamlDumpNodeProps(ts, nodeSetup)
    val mainYamlClass = YamlDumpMainHolder(LocalDateTime.now().toString, snapShotClass)
    val yaml = mainYamlClass.toYaml

    val yamlSource:Source[String, NotUsed] =  Source.single(yaml.prettyPrint)
    val fileName = s"yamldump_${DateTimeFormatter.ISO_DATE.format(LocalDateTime.now()).replace(':','_')}.yaml"

    val yamlResult: Future[IOResult] =
      yamlSource.map(char => ByteString(char))
        .runWith(FileIO.toPath(Paths.get(fileName), fileOpenOptions))
    //logger.info(yaml.prettyPrint)
    context.log.debug("Node YAML done")
  }

  def writeDataYaml(nodeSnapshot: NodeSnapshot, context: ActorContext[Command]): Unit = {
    import com.chord.akkasharding.utils.MyYamlDataProtocol._
    import net.jcazevedo.moultingyaml._

    implicit val system: ActorSystem[Nothing] = context.system
    val nodeSetup = nodeSnapshot.nodeSetup
    val ts = nodeSnapshot.ts.toString
    val dataYamlClass = YamlDumpDataHolder(nodeSetup)
    val dataYaml = dataYamlClass.toYaml

    val yamlSource:Source[String, NotUsed] =  Source.single(dataYaml.prettyPrint)
    val fileName = s"dataYamldump_${DateTimeFormatter.ISO_DATE.format(LocalDateTime.now()).replace(':','_')}.yaml"
    val yamlResult: Future[IOResult] =
      yamlSource.map(char => ByteString(char))
        .runWith(FileIO.toPath(Paths.get(fileName), fileOpenOptions))
    //logger.info(yaml.prettyPrint)
    context.log.debug("Data YAML done")
  }

  def nodeGroupOperations(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>

      message match {
        //added
        case CreateNodes(num_nodes,sharding,typeKey) =>
          context.log.info(s"Creating ${num_nodes} Nodes")
          //added changed ActorRef to EntityRef
          val nodeList = new Array[EntityRef[NodeActor.Command]](SystemConstants.num_nodes)
          for (i <- 0 until SystemConstants.num_nodes) yield {
           val nodeName: String = s"Node_$i"
           //val actorRef = context.spawn(NodeActor(nodeName = nodeName), nodeName)
           val actorRef: EntityRef[NodeActor.Command] = sharding.entityRefFor(typeKey, nodeName)
           nodeList(i) = actorRef
           //NodeList(i) = actorRef.path
            NodeList(i) = nodeName
           if (i == 0) {
             actorRef ! Join(actorRef)
             Thread.sleep(1000)
           }
           else {
             actorRef ! Join(nodeList(0))
             Thread.sleep(30000)
           }
         createdNodes +=  actorRef
         }

          createdNodes.foreach(node => context.log.info(s"Created Nodes are: EnitytRef ${node}"))
          Behaviors.same

        case SaveSnapshot(actorRef) => {
          actorRef ! SaveNodeSnapshot(context.self)

          Behaviors.same
        }

        case ReplySnapshot(nodeSnapshot) => {
          context.log.debug("got snapshot")
          writeYaml(nodeSnapshot, context)
          Behaviors.same
        }

        case ReplyDataSnapshot(nodeSnapshot) =>{
          context.log.debug("got data snapshot")
          writeDataYaml(nodeSnapshot, context)
          Behaviors.same
        }

        case SaveAllSnapshot =>
          createdNodes.toList.foreach(actorRef => actorRef ! SaveNodeSnapshot(context.self))
          Behaviors.same

        case SaveDataSnapshot =>
          createdNodes.toList.foreach(actorRef => actorRef ! SaveNodeDataSnapshot(context.self))
          Behaviors.same
      }


    }


  }



}








