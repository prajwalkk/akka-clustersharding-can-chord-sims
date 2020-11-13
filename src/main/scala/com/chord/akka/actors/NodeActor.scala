package com.chord.akka.actors

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import com.chord.akka.utils.Helper
import com.typesafe.scalalogging.LazyLogging


import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


object NodeActor extends LazyLogging{

  // Requests
  sealed trait Command
  final case class getValues(replyTo: ActorRef[LookupObjects]) extends Command
  final case class addValue(lookupObject: LookupObject, replyTo: ActorRef[ActionSuccessful]) extends Command
  final case class getValue(k: String, replyTo: ActorRef[GetLookupResponse]) extends Command
  final case class Join(nodeRef: ActorRef[NodeActor.Command]) extends Command
  final case class SetPredecessor(nodeRef: ActorRef[Command]) extends Command

  final case object InitFingerTable extends Command

  // Ask messages
  final case class GetFingerTable(replyTo: ActorRef[FingerTableResponse]) extends Command
  //case class FindSuccessor(id: Int, replyTo: ActorRef[SuccessorResponse]) extends Command
  case class GetPredecessor(replyTo: ActorRef[PredecessorResponse]) extends Command


  sealed trait Responses

  // Responses to Self
 // case class SuccessorResponse(actorRef: ActorRef[Command]) extends Command
  case class PredecessorResponse(actorRef: ActorRef[Command]) extends Command
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
    logger.info(s"Initalized fingertable for $nodeName, $nodeId: $fingerTable")

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


  def initFingerTable(currentNodeId: Int,
                      currentFingerTable: List[FingerTableEntity],
                      existingNode: ActorRef[Command],
                      context: ActorContext[Command]): (List[FingerTableEntity], ActorRef[Command], ActorRef[Command]) = {

    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val scheduler: Scheduler = context.system.scheduler

//Changed Code Block
logger.debug(s"Finding Successor")
    val findSuccessor = findPredecessor(context,currentFingerTable.head.start,existingNode)
    val future: Future[FingerTableResponse] = findSuccessor ? GetFingerTable
    val nodeRefFromExistingNode = Await.result(future, timeout.duration).fingerTable.head
logger.debug(s"Find Successor Complete")
//Changed Code Block

//Old Code
//    val future: Future[SuccessorResponse] = existingNode.ask(ref => FindSuccessor(currentFingerTable.head.start, ref))
//    val nodeRefFromExistingNode = Await.result(future, timeout.duration)
//Old code

    val successorNode = nodeRefFromExistingNode.node.get

    val newFingerTableEntity = FingerTableEntity(currentFingerTable.head.start, currentFingerTable.head.startInterval, currentFingerTable.head.endInterval, Some(successorNode))

    val future_2: Future[PredecessorResponse] = successorNode.ask(ref => GetPredecessor(ref))
    val predecessorRefFromSuccessorNode = Await.result(future_2, timeout.duration)
    //Updating Predecessor
    val newPredecessor = predecessorRefFromSuccessorNode.actorRef
    //Updating Predecessor for Successor
    successorNode ! SetPredecessor(existingNode)
    var newList = new Array[FingerTableEntity](currentFingerTable.length)
    newList(0)= newFingerTableEntity

     (0 to 8 - 2).map { i =>
       logger.debug(s"Interval Check ${currentFingerTable(i+1).start} in ${Array(hashNodeRef(existingNode), hashFingerTableEntity(newList(i)))}")
      if (isIdentifierInIntervalRightExclusive(currentFingerTable(i + 1).start, Array(hashNodeRef(existingNode), hashFingerTableEntity(newList(i))))) {
        val newFingerTableListBuilder = FingerTableEntity(currentFingerTable(i).start, currentFingerTable(i).startInterval, currentFingerTable(i).endInterval, newList(i).node)
        newList(i+1)=newFingerTableListBuilder
      }
      else {
        logger.debug("Not in Interval")
//Changed Code Block
        val findSuccessor = findPredecessor(context,currentFingerTable.head.start,existingNode)
        val future_3: Future[FingerTableResponse] = findSuccessor ? GetFingerTable
        val nodeRefFromExistingNode_2 = Await.result(future_3, timeout.duration).fingerTable.head.node.get
//Changed Code Block

//Old code
//        val future_3: Future[SuccessorResponse] = existingNode.ask(ref => FindSuccessor(currentFingerTable(i).start, ref))
//        val nodeRefFromExistingNode_2 = Await.result(future_3, timeout.duration).actorRef
//Old Code
        val newFingerTableListBuilder = FingerTableEntity(currentFingerTable(i).start, currentFingerTable(i).startInterval, currentFingerTable(i).endInterval, Some(nodeRefFromExistingNode_2))
        newList(i+1)=newFingerTableListBuilder
      }
    }
    val finalNewFingerTable =  newList.toList
    logger.debug(s"initFingerTable Done")
    (finalNewFingerTable, newPredecessor, successorNode)
  }

  def join(nodeRef: ActorRef[NodeActor.Command],
           selfAddress: ActorRef[NodeActor.Command],
           oldFingerTable: List[FingerTableEntity],
           currNodeID: Int,
           context: ActorContext[Command]): (List[FingerTableEntity], ActorRef[NodeActor.Command], ActorRef[NodeActor.Command]) = {
    logger.info(s"Node ${context.self.path} Joining Chord Ring")
    if (nodeRef.equals(selfAddress)) {
      // This is when a node is trying to join itself.
      // The first entry
      logger.info("First node Joined")
      val m = 8
      // TODO replace m
      // create a new FingerTable.
      // This is a stateless actor. No mutable variables allowed.
      // Hence, Creating a new table instead of updating each variable
      //Setting finger.node to itself for First Node
      val newFingerTable = oldFingerTable.map { finEntity =>
        FingerTableEntity(finEntity.start, finEntity.startInterval, finEntity.endInterval, Some(nodeRef))
      }
      // return new finger table, predecessor, successor
      // logic for predecessor and successor
      (newFingerTable, nodeRef, nodeRef)

    }
    else {
      // New Actor Joining Finger Table
      logger.debug(s"initFingerTable for node ${selfAddress.path} ")
      initFingerTable(currNodeID, oldFingerTable, nodeRef, context)

      // TODO update others
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


        case GetFingerTable(replyTo) => {

          replyTo ! FingerTableResponse(fingerTable)
          Behaviors.same
        }

        case GetPredecessor(replyTo) => {

          replyTo ! PredecessorResponse(predecessor.get)
          Behaviors.same
        }

        case SetPredecessor(newPredecessor) => {
          nodeBehaviors(nodeID, lookupObjectSet, fingerTable, Some(newPredecessor), successor)
        }

        case Join(nodeRef) =>
          /*
          * When a new node joins, the following values MAY change in the node that receives the Join request
          * fingerTable, Predecessor Successor,
          * */
          val selfAddress = context.self

          val (newFingerTable, newPredecessor, newSuccessor): (List[FingerTableEntity], ActorRef[NodeActor.Command], ActorRef[NodeActor.Command]) = join(nodeRef, selfAddress, fingerTable, nodeID, context)
          newFingerTable.foreach(i => context.log.debug(s"Updated fingerTable: $i"))
          context.log.info(s"Finger Table updated for ${context.self.path}")
          nodeBehaviors(nodeID, lookupObjectSet, newFingerTable, Some(newPredecessor), Some(newSuccessor))

//Not Required Functionality added to 'Changed Code Block'
//        case FindSuccessor(id, replyTo) =>
//          //findSuccessor(id, fingerTable)
//          val n_dash = findPredecessor(context, id)
//          // find the successor of the predecessor
//          implicit val timeout: Timeout = Timeout(3.seconds)
//          implicit val scheduler: Scheduler = context.system.scheduler
//          val future: Future[FingerTableResponse] = n_dash ? GetFingerTable
//          val resp = Await.result(future, timeout.duration)
//          replyTo ! SuccessorResponse(resp.fingerTable.head.node.get)
//          Behaviors.same
      }
    }
  }


  def hashFingerTableEntity(f: FingerTableEntity): Int = {
    Helper.getIdentifier(f.node.get.path.toString.split("/").last)
  }

  def hashNodeRef(f: ActorRef[Command]): Int = {
    Helper.getIdentifier(f.path.toString.split("/").toSeq.last)
  }

  // TODO This function might be a Point of Failure
  def findPredecessor(context: ActorContext[Command], id: Int,existingNode:ActorRef[Command]): ActorRef[NodeActor.Command] = {
    logger.debug(s"Finding Predecessor")
    var nDash = existingNode

    def getIntermediateValues(nodeDash: ActorRef[Command]): List[FingerTableEntity] = {
      implicit val timeout: Timeout = Timeout(10.seconds)
      implicit val scheduler: Scheduler = context.system.scheduler
      val future: Future[FingerTableResponse] = nodeDash ? GetFingerTable
      val resp = Await.result(future, timeout.duration)
      resp.fingerTable
    }

    var nDashFingerTable = getIntermediateValues(nDash)
    var nDashSuccessor = Helper.getIdentifier(nDashFingerTable.head.node.get.path.toString.split("/").toSeq.last)
    var nDashNode = Helper.getIdentifier(nDash.path.toString.split("/").toSeq.last)
    while (!isIdentifierInIntervalLeftExclusive(id, Array(nDashNode, nDashSuccessor))) {

      nDash = closestPrecedingFinger(id, nDashFingerTable, nDash)
      nDashFingerTable = getIntermediateValues(nDash)
      nDashSuccessor = hashFingerTableEntity(nDashFingerTable.head)
      nDashNode = hashNodeRef(nDash)
    }
    logger.debug(s"Finding Predecessor Complete")
    nDash

  }

  def closestPrecedingFinger(id: Int, nodeFingerTable: List[FingerTableEntity], node: ActorRef[Command]): ActorRef[Command] = {
    // TODO change m
    val m = 8
    for (i <- (m - 1) to 0) {
      if (isIdentifierInIntervalBothExclusive(id, Array(hashFingerTableEntity(nodeFingerTable(i)), hashNodeRef(node)))) {
        return nodeFingerTable(i).node.get
      }
    }
    node
  }




  def isIdentifierInIntervalLeftExclusive(identifier: Int, interval: Array[Int]): Boolean = {
    val bitSize = 8
    //TODO
    logger.debug(s"Interval Checking till found  ${identifier} in ${interval}")
    if (interval(0) < interval(1)) {
      if (identifier > interval(0) && identifier <= interval(1))
        return true
      else
        return false
    }
    val interval1: Array[Int] = Array(interval(0), Math.pow(2, bitSize).asInstanceOf[Int])
    val interval2: Array[Int] = Array(0, interval(1))
    isIdentifierInIntervalLeftExclusive(identifier, interval1) || isIdentifierInIntervalLeftExclusive(identifier, interval2)
  }

  def isIdentifierInIntervalRightExclusive(identifier: Int, interval: Array[Int]): Boolean = {
    val bitSize = 8
    //TODO
    logger.debug(s"Interval Checking till found  ${identifier} in ${interval}")
    if (interval(0) < interval(1)) {
      if (identifier >= interval(0) && identifier < interval(1)) {

        return true
      } else {

        return false
      }
    }
    val interval1: Array[Int] = Array(interval(0), Math.pow(2, bitSize).asInstanceOf[Int])
    val interval2: Array[Int] = Array(0, interval(1))
    isIdentifierInIntervalRightExclusive(identifier, interval1) || isIdentifierInIntervalRightExclusive(identifier, interval2)
  }

  def isIdentifierInIntervalBothExclusive(identifier: Int, interval: Array[Int]): Boolean = {
    val bitSize = 8
    //TODO
    logger.debug(s"Interval Checking till found  ${identifier} in ${interval}")
    if (interval(0) < interval(1)) {
      if (identifier > interval(0) && identifier < interval(1))
        return true
      else
        return false
    }
    val interval1: Array[Int] = Array(interval(0), Math.pow(2, bitSize).asInstanceOf[Int])
    val interval2: Array[Int] = Array(0, interval(1))
    isIdentifierInIntervalBothExclusive(identifier, interval1) || isIdentifierInIntervalBothExclusive(identifier, interval2)
  }
}





