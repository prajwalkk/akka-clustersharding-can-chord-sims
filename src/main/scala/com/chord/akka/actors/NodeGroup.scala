package com.chord.akka.actors

import java.time.LocalDateTime

import akka.actor.ActorPath
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent._
import java.nio.file.{OpenOption, Paths, StandardOpenOption}

import akka.NotUsed
import akka.util.{ByteString, Timeout}
import com.chord.akka.actors.NodeActorTest.{Join, NodeSetup, PrintUpdate, SaveNodeSnapshot}
import com.chord.akka.utils.{SystemConstants, YamlDumpFingerTableEntity, YamlDumpMainHolder, YamlDumpNodeProps}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
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
          writeYaml(nodeSnapshot, context)
          Behaviors.same
        }
      }

    }


  }


  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  def writeYaml(nodeSnapshots: NodeSnapshot, context: ActorContext[Command]): Unit ={
    import net.jcazevedo.moultingyaml._
    import com.chord.akka.utils.MyYamlProtocol._


    implicit val system: ActorSystem[Nothing] = context.system
    val nodeSetup = nodeSnapshots.nodeSetup
    val ts = nodeSnapshots.ts.toString
    val snapShotClass = YamlDumpNodeProps(ts, nodeSetup)
    val mainYamlClass = YamlDumpMainHolder(LocalDateTime.now().toString, snapShotClass)
    val yaml = mainYamlClass.toYaml

    val yamlSource:Source[String, NotUsed] =  Source.single(yaml.prettyPrint)
    val fileName = s"yamldump_${LocalDateTime.now().toLocalDate.toString}"
    val fileOpenOptions: Set[OpenOption] = Set(StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)
    val yamlResult: Future[IOResult] =
      yamlSource.map(char => ByteString(char))
        .runWith(FileIO.toPath(Paths.get(fileName), fileOpenOptions))
    //logger.info(yaml.prettyPrint)
    context.log.debug("YAML done")
  }


}








