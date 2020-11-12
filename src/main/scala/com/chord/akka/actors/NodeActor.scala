package com.chord.akka.actors

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern.{schedulerFromActorSystem, _}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout
import com.chord.akka.utils.Helper
import com.typesafe.scalalogging.LazyLogging
import jdk.internal.logger.DefaultLoggerFinder.SharedLoggers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/*
*
* Created by: prajw
* Date: 04-Nov-20
*
*/


object NodeActor extends LazyLogging{

  // Requests
  sealed trait Command
  final case class getValues(replyTo: ActorRef[LookupObjects]) extends Command
  final case class addValue(lookupObject: LookupObject, replyTo: ActorRef[ActionSuccessful]) extends Command
  final case class getValue(k: String, replyTo: ActorRef[GetLookupResponse]) extends Command
  final case class Join(nodeRef: ActorRef[NodeActor.Command]) extends Command
  final case class GetFingerTable(replyTo: ActorRef[Responses]) extends Command
  final case object InitFingerTable extends Command
  final case class FindSuccessor(id: Int) extends Command


  sealed trait Responses
  case class FingerTableResponse(fingerTable: List[FingerTableEntity]) extends Responses
  //Responses
  case class ActionSuccessful(description: String)
  case class GetLookupResponse(maybeObject: Option[LookupObject])

  def apply(nodeName: String): Behavior[Command] = {
    //    val fingerTable = ???
    //    val successor = ???
    //    val predecessor = ???
    // val data

    val nodeId = Helper.getIdentifier(nodeName)
    val fingerTable = getTemplateFingerTable(nodeId)
    logger.debug(s"Initalized fingertable for $nodeName, $nodeId: $fingerTable")

    nodeBehaviors(nodeId, Set.empty, fingerTable, None, None)
  }

  def getTemplateFingerTable(nodeId: Int): List[FingerTableEntity] = {
    // TODO replace m
    val m = 8
    val fingerTableList = for (k <- 1 to m) yield {
      val startVal: Int = (nodeId + Math.pow(2, k - 1).toInt) % Math.pow(2, m).toInt
      val endVal: Int = if (k == m) nodeId else (nodeId + Math.pow(2, k).toInt) % Math.pow(2, m).toInt

      //TODO update Node
      FingerTableEntity(startVal, startInterval = startVal, endInterval = endVal, node = None)
    }
    fingerTableList.toList
  }


  def initFingerTable(currentFingerTable: List[FingerTableEntity],
                      existingNode: ActorRef[Command]): (List[FingerTableEntity], ActorRef[Command], ActorRef[Command]) = {
    implicit val timeout: Timeout = Timeout(3.seconds)
    //val future = existingNode.ask(FindSuccessor(currentFingerTable(0).start))
    val nodeRefFromExistingNode = Await.result(future, timeout.duration)
    val newFingerTable = FingerTableEntity(currentFingerTable(0).start, currentFingerTable(0).startInterval, currentFingerTable(0).endInterval, nodeRefFromExistingNode)
  }

  def join(nodeRef: ActorRef[Command],
           selfAddress: ActorRef[Command],
           oldFingerTable: List[FingerTableEntity],
           currNodeID: Int): (List[FingerTableEntity], ActorRef[NodeActor.Command], ActorRef[NodeActor.Command]) = {
    if (nodeRef.equals(selfAddress)) {
      // This is when a node is trying to join itself.
      // The first entry
      logger.info("Same node trying to join")
      val m = 8
      // TODO replace m
      // create a new FingerTable.
      // This is a stateless actor. No mutable variables allowed.
      // Hence, Creating a new table instead of updating each variable
      val newFingerTable = oldFingerTable.map { finEntity =>
        FingerTableEntity(finEntity.start, finEntity.startInterval, finEntity.endInterval, Some(nodeRef))
      }
      // return new finger table, predecessor, successor
      // logic for predecessor and successor
      (newFingerTable, nodeRef, nodeRef)

    }
    else {
      // If new actor tries to join the Fingertable
      initFingerTable(oldFingerTable, nodeRef)
      // TODO
      //(oldFingerTable, 0, 0)

    }

  }

  def isIdentifierInInterval(identifier: Int, interval: Array[Int]): Boolean = {
    if (interval(0) < interval(1)) false
    else interval(0) until interval(1) contains identifier
  }

  /**
   * Specifies the default behavior of a Node in Chord. It is initlaized with a finger table without any values.
   * waits for messages from the parent node Group or other nodes or from web server
   * holds a fingertable, successor, id, predecessor and a datamap
   *
   * @param nodeID          ID of that node [Int]
   * @param lookupObjectSet data that is stored in that node
   * @param fingerTable     finger table List[[FingerTableEntity]]
   * @param predecessor     predecessor of that node [Int]
   * @param successor       successor of the node[Int]
   * @return
   */
  def nodeBehaviors(nodeID: Int,
                    lookupObjectSet: Set[LookupObject],
                    fingerTable: List[FingerTableEntity],
                    predecessor: Option[ActorRef[NodeActor.Command]],
                    successor: Option[ActorRef[NodeActor.Command]]): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {


        case getValues(replyTo: ActorRef[LookupObjects]) =>
          context.log.info(s"My name is ${context.self}")
          replyTo ! LookupObjects(lookupObjectSet.toSeq)
          Behaviors.same

        case addValue(lookupObject, replyTo) =>
          replyTo ! ActionSuccessful(s"object ${lookupObject.key} created")
          nodeBehaviors(nodeID, lookupObjectSet + lookupObject, fingerTable, predecessor, successor)

        case getValue(k, replyTo) =>
          replyTo ! GetLookupResponse(lookupObjectSet.find(_.key == k))
          Behaviors.same

        case Join(nodeRef) =>
          /*
          * When a new node joins, the following values MAY change in the node that receives the Join request
          * fingerTable, Predecessor Successor,
          * */
          val selfAddress = context.self
          val (newFingerTable, newPredecessor, newSuccessor): (List[FingerTableEntity], ActorRef[NodeActor.Command], ActorRef[NodeActor.Command]) = join(nodeRef, selfAddress, fingerTable, nodeID)
          newFingerTable.foreach(i => context.log.info(s"Updated fingerTable: $i"))
          nodeBehaviors(nodeID, lookupObjectSet, newFingerTable, Some(newPredecessor), Some(newSuccessor))


        case FindSuccessor(id:Int) =>
          //findSuccessor(id, fingerTable)
          val n_dash = findPredecessor(id)
          implicit val timeout: Timeout = Timeout(3.seconds)
          implicit val scheduler: Scheduler = context.system.scheduler
          val future: Future[Responses] = n_dash ? GetFingerTable
          val resp = Await.result(future, timeout.duration).asInstanceOf[FingerTableResponse]
          context.log.info("YOLO {}", resp.fingerTable)
          Behaviors.same

        case InitFingerTable => {
          implicit val timeout: Timeout = Timeout(3.seconds)
          val future = existingNode.ask(FindSuccessor(currentFingerTable(0).start))
          val nodeRefFromExistingNode = Await.result(future, timeout.duration)
          val newFingerTable = FingerTableEntity(currentFingerTable(0).start, currentFingerTable(0).startInterval, currentFingerTable(0).endInterval, nodeRefFromExistingNode)
        }



      }
    }
  }



//  def findSuccessor(id: Int, value: List[FingerTableEntity]): ActorRef[NodeActor.Command] = {
//
//    implicit def schedulerFromActorSystem(system: ActorSystem[NotUsed]): Scheduler = system.scheduler
//
//    Await.result(future, timeout.duration)
//  }
  def findPredecessor(id: Int): ActorRef[NodeActor.Command] = ???
  def closestPreceedingFinger(id: Int) = ???

  /* def findPredecessor(identifier: Int, selfActorRef: ActorRef[NodeActor.Command]): ActorRef[NodeActor.Command] ={
     //if(isIdentifierInInterval(identifier, Array()))
   }*/

}