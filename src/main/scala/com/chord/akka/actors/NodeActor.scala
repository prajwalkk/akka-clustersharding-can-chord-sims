package com.chord.akka.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.chord.akka.utils.Helper
import com.typesafe.scalalogging.LazyLogging

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

  //Responses
  case class ActionSuccessful(description: String)
  case class GetLookupResponse(maybeObject: Option[LookupObject])

  def apply(nodeName: String): Behavior[Command] = {
    //    val fingerTable = ???
    //    val successor = ???
    //    val predecessor = ???
    // val data

    val nodeId = Helper.getIdentifier(nodeName)
    val fingerTable  = initTemplateFingerTable(nodeId)
    logger.info(s"Initalized fingertable for ${nodeName}, ${nodeId}: ${fingerTable}")

    nodeBehaviors(nodeId, Set.empty, fingerTable)
  }

  def initTemplateFingerTable(nodeId: Int): List[FingerTableEntity] = {
    // TODO replace m
    val m = 8
    val fingerTableList = for (k <- 1 to m) yield{
      val startVal: Int = (nodeId + Math.pow(2, k - 1).toInt) % Math.pow(2, m).toInt
      val endVal: Int = if (k == m) nodeId else (nodeId + Math.pow(2, k).toInt) % Math.pow(2, m).toInt
      FingerTableEntity(startVal, startInterval = startVal, endInterval = endVal,node = nodeId, 0, 0)
    }
    fingerTableList.toList
  }


  def join(nodeRef: ActorRef[Command], selfAddress: ActorRef[Command], oldFingerTable: List[FingerTableEntity], currNodeID: Int): List[FingerTableEntity] = {
    if (nodeRef.equals(selfAddress)) {
      logger.info("Same node trying to join")
      val m = 8
      // TODO replace m
      // create a new FingerTable
      val newFingerTable = oldFingerTable.map { finEntity => FingerTableEntity(finEntity.start, finEntity.startInterval, finEntity.endInterval, finEntity.node, currNodeID, currNodeID)
      }
      newFingerTable
    }
    else {
      // If new actor tries to join the Fingertable
      initFingerTable(oldFingerTable, currNodeID)
      // TODO
      oldFingerTable
    }

  }

  def nodeBehaviors(nodeID: Int, lookupObjectSet: Set[LookupObject], fingerTable: List[FingerTableEntity]): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case getValues(replyTo: ActorRef[LookupObjects]) =>
          context.log.info(s"My name is ${context.self}")
          replyTo ! LookupObjects(lookupObjectSet.toSeq)
          Behaviors.same

        case addValue(lookupObject, replyTo) =>
          replyTo ! ActionSuccessful(s"object ${lookupObject.key} created")
          nodeBehaviors(nodeID,lookupObjectSet + lookupObject, fingerTable)

        case getValue(k, replyTo) =>
          replyTo ! GetLookupResponse(lookupObjectSet.find(_.key == k))
          Behaviors.same

        case Join(nodeRef) => {
          val selfAddress = context.self
          val newFingerTable: List[FingerTableEntity] = join(nodeRef, selfAddress, fingerTable, nodeID)
          newFingerTable.foreach(i => context.log.info(s"Updated fingerTable: ${i}"))
          nodeBehaviors(nodeID, lookupObjectSet, newFingerTable)
        }
      }
    }
  }

 /* def findPredecessor(identifier: Int, selfActorRef: ActorRef[NodeActor.Command]): ActorRef[NodeActor.Command] ={
    //if(isIdentifierInInterval(identifier, Array()))
  }*/

  def isIdentifierInInterval(identifier: Int, interval: Array[Int]): Boolean = {
     if (interval(0) < interval(1) ) false
     else (interval(0) until interval(1) contains identifier)
  }




}