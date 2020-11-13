package com.chord.akka.actors

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import com.chord.akka.utils.Helper
import com.typesafe.scalalogging.LazyLogging


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

  final case object InitFingerTable extends Command

  // Ask messages
  final case class GetFingerTable(replyTo: ActorRef[FingerTableResponse]) extends Command
  case class FindSuccessor(id: Int, replyTo: ActorRef[SuccessorResponse]) extends Command


  sealed trait Responses

  // Responses to Self
  case class SuccessorResponse(actorRef: ActorRef[Command]) extends Command
  case class FingerTableResponse(fingerTable: List[FingerTableEntity]) extends Command

  //Responses to Other actors
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
                      existingNode: ActorRef[Command],
                      context: ActorContext[Command]): (List[FingerTableEntity], ActorRef[Command], ActorRef[Command]) = {
    implicit val timeout: Timeout = Timeout(3.seconds)
    implicit val scheduler: Scheduler = context.system.scheduler
    val future: Future[SuccessorResponse] = existingNode.ask(ref => FindSuccessor(currentFingerTable.head.start, ref))
    val nodeRefFromExistingNode = Await.result(future, timeout.duration)
    val newFingerTable = FingerTableEntity(currentFingerTable.head.start, currentFingerTable.head.startInterval, currentFingerTable.head.endInterval, Some(nodeRefFromExistingNode.actorRef))
    (currentFingerTable, existingNode, existingNode)
  }

  def join(nodeRef: ActorRef[NodeActor.Command],
           selfAddress: ActorRef[NodeActor.Command],
           oldFingerTable: List[FingerTableEntity],
           currNodeID: Int,
           context: ActorContext[Command]): (List[FingerTableEntity], ActorRef[NodeActor.Command], ActorRef[NodeActor.Command]) = {
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
      initFingerTable(oldFingerTable, nodeRef, context)
      // TODO
      //(oldFingerTable, 0, 0)

    }

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
          val (newFingerTable, newPredecessor, newSuccessor): (List[FingerTableEntity], ActorRef[NodeActor.Command], ActorRef[NodeActor.Command]) = join(nodeRef, selfAddress, fingerTable, nodeID, context)
          newFingerTable.foreach(i => context.log.info(s"Updated fingerTable: $i"))
          nodeBehaviors(nodeID, lookupObjectSet, newFingerTable, Some(newPredecessor), Some(newSuccessor))


        case FindSuccessor(id, replyTo) =>
          //findSuccessor(id, fingerTable)
          val n_dash = findPredecessor(context, id)
          // TODO successor
          replyTo ! SuccessorResponse(n_dash)
          Behaviors.same

        case GetFingerTable(replyTo) => {
          replyTo ! FingerTableResponse(fingerTable)
          Behaviors.same
        }

      }
    }
  }



  // TODO This function might be a Point of Failure
  def findPredecessor(context: ActorContext[Command], id: Int): ActorRef[NodeActor.Command] = {
    var nDash = context.self

    def getIntermediateValues(nodeDash: ActorRef[Command]): List[FingerTableEntity] = {
      implicit val timeout: Timeout = Timeout(3.seconds)
      implicit val scheduler: Scheduler = context.system.scheduler
      val future: Future[FingerTableResponse] = nodeDash ? GetFingerTable
      val resp = Await.result(future, timeout.duration)
      resp.fingerTable
    }

    var nDashFingerTable = getIntermediateValues(nDash)
    var nDashSuccessor = Helper.getIdentifier(nDashFingerTable.head.node.get.path.toString.split("/").toSeq.last)
    var nDashNode = Helper.getIdentifier(nDash.path.toString.split("/").toSeq.last)
    while (!isIdentifierInInterval(id, Array(nDashNode, nDashSuccessor))) {

      nDash = closestPrecedingFinger(id, nDashFingerTable, nDash)
      nDashFingerTable = getIntermediateValues(nDash)
      nDashSuccessor = hashFingerTableEntity(nDashFingerTable.head)
      nDashNode = hashNodeRef(nDash)
    }
    nDash

  }
  def closestPrecedingFinger(id: Int, nodeFingerTable: List[FingerTableEntity], node: ActorRef[Command]): ActorRef[Command] ={
    // TODO change m
    val m = 8
    for(i <- (m - 1) to 0){
      if(isIdentifierInInterval(id, Array(hashFingerTableEntity(nodeFingerTable(i)), hashNodeRef(node)))){
        return nodeFingerTable(i).node.get
      }
    }
    node
  }

  def hashFingerTableEntity(f: FingerTableEntity): Int ={
    Helper.getIdentifier(f.node.get.path.toString.split("/").last)
  }
  def hashNodeRef(f: ActorRef[Command]): Int = {
    Helper.getIdentifier(f.path.toString.split("/").toSeq.last)
  }

  def isIdentifierInInterval(identifier: Int, interval: Array[Int]): Boolean = {
    val bitSize = 8
    //TODO
    if (interval(0) < interval(1)) {
      if (identifier > interval(0) && identifier <= interval(1))
        return true
      else
        return false
    }
    val interval1: Array[Int] = Array(interval(0), Math.pow(2, bitSize).asInstanceOf[Int])
    val interval2: Array[Int] = Array(0, interval(1))
    isIdentifierInInterval(identifier, interval1) || isIdentifierInInterval(identifier, interval2)
  }
}





