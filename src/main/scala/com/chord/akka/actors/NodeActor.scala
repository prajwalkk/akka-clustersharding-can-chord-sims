package com.chord.akka.actors

import java.net.URLDecoder
import java.time.LocalDateTime

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import com.chord.akka.actors.NodeGroup.{NodeSnapshot, ReplySnapshot, ReplyWithJoinStatus}
import com.chord.akka.simulation.Simulation.select_random_node
import com.chord.akka.utils.{Helper, SystemConstants}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

/*
*
* Created by: prajw
* Date: 13-Nov-20
*
*/
object NodeActor extends LazyLogging {

  def apply(nodeName: String): Behavior[Command] = {
    // Each node has a:
    // Node ID
    // Node Name
    // Node Successor
    // Node Predecessor
    // FingerTable

    Behaviors.setup { context =>
      val nodeId = Helper.getIdentifier(nodeName)
      val nodeFingerTable = getTemplateFingerTable(nodeId)
      val nodeRef = context.self
      val nodePropertiesWhenCreated = NodeSetup(
        nodeName = nodeName,
        nodeID = nodeId,
        nodeRef = nodeRef,
        nodeSuccessor = None,
        nodePredecessor = None,
        nodeFingerTable = nodeFingerTable,
        storedData = mutable.HashMap.empty)
      new NodeActor(nodeName, context).nodeBehaviors(nodePropertiesWhenCreated)
    }

  }

  // This is just to set up a node when it is created the first time
  private def getTemplateFingerTable(id: Int): List[FingerTableEntity] = {
    val m = SystemConstants.M
    val fingerTableList = (1 to m).map { k =>
      val startVal: Int = (id + Math.pow(2, k - 1).toInt) % Math.pow(2, m).toInt
      val endVal: Int = if (k == m) id else (id + Math.pow(2, k).toInt) % Math.pow(2, m).toInt
      FingerTableEntity(start = startVal, startInterval = startVal, endInterval = endVal, node = None)
    }
    fingerTableList.toList
  }

  // Protocols
  sealed trait Command

  // ask
  final case class FindSuccessor(id: Int,
                                 replyTo: ActorRef[ReplyWithSuccessor]) extends Command

  // reply
  final case class ReplyWithSuccessor(nSuccessor: NodeSetup) extends Command

  // ask
  final case class FindPredecessor(id: Int,
                                   replyTo: ActorRef[ReplyWithPredecessor]) extends Command

  // reply
  final case class ReplyWithPredecessor(nodeSetup: NodeSetup) extends Command

  // ask
  final case class ClosestPrecedingFinger(id: Int,
                                          replyTo: ActorRef[ReplyWithClosestPrecedingFinger]) extends Command

  final case class ReplyWithClosestPrecedingFinger(nodeSetup: NodeSetup) extends Command

  // ask
  final case class GetNodeProperties(replyTo: ActorRef[ReplyWithNodeProperties]) extends Command

  final case class ReplyWithNodeProperties(nodeSetup: NodeSetup) extends Command

  // nDash arbitrary node in the network
  final case class Join(nDash: ActorRef[NodeActor.Command], replyTo: ActorRef[NodeGroup.ReplyWithJoinStatus]) extends Command
  final case class InitFingerTable(nDash: ActorRef[NodeActor.Command]) extends Command

  // tell a node to update its Predecessor to the passed node
  final case class SetPredecessor(nodeRef: ActorRef[NodeActor.Command], replyTo: ActorRef[NodeActor.ReplyPredecessorConfirmation]) extends Command
  // Response to the above message
  final case class ReplyPredecessorConfirmation(Str: String) extends Command

  // update all nodes
  final case class UpdateFingerTable(s: NodeSetup,
                                     i: Int) extends Command

  final case class SaveNodeSnapshot(replyTo: ActorRef[NodeGroup.ReplySnapshot]) extends Command

  final case class FindNode(requestObject: RequestObject, replyTo: ActorRef[ActionSuccessful]) extends Command

  final case class SearchDataNode(lookup: String, replyTo: ActorRef[ActionSuccessful]) extends Command

  final case class getValues(replyTo: ActorRef[LookupObjects]) extends Command

  final case class addValue(requestObject: RequestObject, replyTo: ActorRef[ActionSuccessful]) extends Command

  final case class getValue(k: String, replyTo: ActorRef[GetLookupResponse]) extends Command

  //Responses to Other actors
  case class ActionSuccessful(description: String)

  case class GetLookupResponse(maybeObject: Option[LookupObject])

  // Use this instead of passing way too many parameters
  case class NodeSetup(nodeName: String,
                       nodeID: Int,
                       nodeRef: ActorRef[NodeActor.Command],
                       nodeSuccessor: Option[ActorRef[NodeActor.Command]],
                       nodePredecessor: Option[ActorRef[NodeActor.Command]],
                       nodeFingerTable: List[FingerTableEntity],
                       storedData: mutable.HashMap[Int, String]

                      ) {
    override def toString: String = {
      s"node :$nodeName \tnodeRef :${nodeRef.path} " +
        s"\tnodeSuccessor : ${nodeSuccessor.get.path.name} " +
        s"\tnodePredecessor :${nodePredecessor.get.path.name}," +
        s"\tnodeFingerTable: \t${
          nodeFingerTable.map(_.toString
          )
        }"
    }

  }

  final case object UpdateOthers extends Command

  final case object printUpdate extends Command

  // Print and snapshotting
  final case object PrintUpdate extends Command

}


class NodeActor private(name: String,
                        context: ActorContext[NodeActor.Command]) {

  import NodeActor._


  def rangeValidator(leftInclude: Boolean, leftValue: BigInt, rightValue: BigInt, rightInclude: Boolean, value: BigInt): Boolean = {


    if (leftValue == rightValue) {
      true
    } else if (leftValue < rightValue) {
      if (value == leftValue && leftInclude || value == rightValue && rightInclude || (value > leftValue && value < rightValue)) {
        true
      } else {
        false
      }
    } else {
      if (value == leftValue && leftInclude || value == rightValue && rightInclude || (value > leftValue || value < rightValue)) {
        true
      } else {
        false
      }
    }
  }

  private def nodeBehaviors(nodeProperties: NodeSetup): Behavior[Command] = {
    // We already have access to context. Hence
    Behaviors.receiveMessage {
      case Join(nDash, actorRefToReply) => {
        // This message is called by a new node that joins.
        // It has the reference of the existing node(nDash).
        // This code is being executed in the new node (n)
        // n ! Join(nDash)
        // at the first case, the caller and callee will be the same node
        // (initial node joins an empty network) the else part of the algo
        val n = context.self
        val nFingerTable = nodeProperties.nodeFingerTable
        if (n.equals(nDash)) {
          context.log.debug(s"[$name] Same node joining: $n with $nDash")
          // Set the finger Table to itself
          val newFingerTable = (1 to SystemConstants.M).map { i =>
            val newFingerEntity = nFingerTable(i - 1).copy(node = Some(n))
            newFingerEntity
          }.toList
          // change the predecessor to N
          val newPredecessor = n
          val newSuccessor = newFingerTable.head.node.get
          // The node properties Changed
          val newSetup = nodeProperties.copy(
            nodeFingerTable = newFingerTable,
            nodePredecessor = Some(newPredecessor),
            nodeSuccessor = Some(newSuccessor)
          )
          context.log.info(s"[$name]Update node Properties: ${newSetup.toString}")
          actorRefToReply ! ReplyWithJoinStatus("joinSuccess")
          nodeBehaviors(newSetup)
        } else {
          context.log.info(s"[$name]Different node calling node 0")
          // if it is a different node
          // n ! Join(nDash)
          // n ! InitFingerTable(nDash)
          //context.log.info(s"[${context.self.path.name}] Init FingerTable Old props: ${nodeProperties.toString}")
          val newNodeProperties = init_finger_table(nDash, nodeProperties)
          context.log.debug(s"[${context.self.path.name}] Init FingerTable: ${newNodeProperties.toString} ")
          //nodeBehaviors(newNodeProperties)

          context.log.debug(s"${context.self.path} calling update others in Init")
          val newNodeAfterUpdate =  updateOthers(newNodeProperties)
          context.log.debug(s"[$name]Update node Properties: ${newNodeAfterUpdate.getOrElse(0).toString}")
          if(newNodeAfterUpdate.getOrElse(0) == 0){
            context.log.debug("blejh blejfh if")
            actorRefToReply ! ReplyWithJoinStatus("joinSuccess")
            nodeBehaviors(newNodeProperties)
          }else{
            context.log.debug("bleh blejfh else")
            actorRefToReply ! ReplyWithJoinStatus("joinSuccess")
            context.log.debug("YPLO"+ newNodeAfterUpdate.get.toString)
            context.log.debug("YPLO"+ newNodeProperties.toString)
            nodeBehaviors(newNodeAfterUpdate.get)
          }
        }
      }

      case FindSuccessor(id, replyTo) => {
        // TODO return ndash.successor
        val nDash = find_predecessor(id, nodeProperties.copy())
        val nDashRef = nDash.nodeRef
        implicit val timeout: Timeout = Timeout(3.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler
        if (nDashRef.equals(context.self)) {
          // If nDash is myself
          val nDashSuccessorNodeRef = nodeProperties.nodeSuccessor.get
          if (nDashSuccessorNodeRef.equals(context.self)) {
            // if nDash.successor is myself reply with my node props
            replyTo ! ReplyWithSuccessor(nodeProperties.copy())
          } else {
            // if nDash (who is self) and his successor is someone else
            val future2: Future[ReplyWithNodeProperties] = nDashSuccessorNodeRef.ask(ref => GetNodeProperties(ref))
            val nDashSuccessorNodeSetup = Await.result(future2, timeout.duration).nodeSetup
            replyTo ! ReplyWithSuccessor(nDashSuccessorNodeSetup)
          }
          // TODO wrong logic. return successor or yourself.
          // replyTo ! ReplyWithSuccessor(nDashSuccessor)
        } else {
          // if nDash is not me (some other node)
          val future: Future[ReplyWithNodeProperties] = nDashRef.ask(ref => GetNodeProperties(ref))
          val nDashNodeSetup = Await.result(future, timeout.duration).nodeSetup
          val nDashSuccessorNodeRef = nDashNodeSetup.nodeSuccessor.get
          if(nDashSuccessorNodeRef.equals(context.self)){
            // if the successor of nDash is me directly return my properties
            // TODO check with Rishabh
            replyTo ! ReplyWithSuccessor(nodeProperties.copy())
          }else{
            // If the nDash is not me and nDash sucessor is not me
            val future2:Future[ReplyWithNodeProperties] = nDashSuccessorNodeRef.ask(ref => GetNodeProperties(ref))
            val nDashSuccessorNodeSetup = Await.result(future2, timeout.duration).nodeSetup
            replyTo ! ReplyWithSuccessor(nDashSuccessorNodeSetup)
          }
        }
        Behaviors.same
      }

      case FindPredecessor(idActorRef, replyTo: ActorRef[NodeActor.Command]) => {
        context.log.info(s"[${context.self.path.name}] Received a find Predecessor request from ${replyTo.path.name}")
        val nDash = find_predecessor(idActorRef, nodeProperties)
        replyTo ! ReplyWithPredecessor(nDash.copy())
        context.log.info(s"[${context.self.path.name}] in Find Predecessor. Got result and replied to: ${replyTo.path.name} ")
        Behaviors.same
      }

      case ClosestPrecedingFinger(id, replyTo) => {
        context.log.debug(s"[${context.self.path.name}] Executing closest preceding finger. replyTo: ${replyTo.path.name} ")
        val n = closest_preceding_finger(id, nodeProperties)
        replyTo ! ReplyWithClosestPrecedingFinger(n)
        context.log.debug(s"[${context.self.path.name}] Executed n = ${n.toString}. Reply done: ${replyTo.path.name} ")
        Behaviors.same
      }

      case InitFingerTable(nDash) => {
        context.log.info(s"[${context.self.path.name}] Init FingerTable Old props: ${nodeProperties.toString}")
        val newNodeProperties = init_finger_table(nDash, nodeProperties)
        context.log.info(s"[${context.self.path.name}] Init FingerTable: ${newNodeProperties.toString} ")
        nodeBehaviors(newNodeProperties)
      }
      case SetPredecessor(nodeRef, replyTo) => {
        context.log.info(s"[${context.self.path.name}] Setting my predecessor to ${nodeRef.path.name}")
        val newPredecessor = nodeRef
        val newNodeProps = nodeProperties.copy(nodePredecessor = Some(newPredecessor))
        context.log.info(s"[${context.self.path.name}] New Props to ${newNodeProps.toString}")
        replyTo ! ReplyPredecessorConfirmation("ok")
        nodeBehaviors(newNodeProps)
      }

      case UpdateOthers => {
        context.log.debug(s"UpdateOthers with ${context.self}")
        val n = nodeProperties.copy()

        for (i <- 0 until SystemConstants.M) {
          val idTemp = n.nodeID - Math.pow(2, i).toInt
          val id = if(idTemp < 0) (idTemp + Math.pow(2, SystemConstants.M).toInt) else idTemp
          context.log.debug(s"in update others ${context.self.path.name}  findPredecessor($id) ${n.nodeID}")
          val pNodeSetup = find_predecessor(n.nodeID - Math.pow(2, i).toInt, n)
          context.log.debug(s" print i : $i ")
          val p = pNodeSetup.nodeRef
          context.log.debug(s"Calling update finger table p: ${p.path.name} i:$i  ")
          p ! UpdateFingerTable(n, i)
        }
        Behaviors.same
      }

      case UpdateFingerTable(sNodeSetup: NodeSetup, i: Int) => {
        context.log.debug(s"In Update Finger Table of ${context.self.path.name}")
        val s = sNodeSetup.nodeID
        val n = nodeProperties
        val nFinger = nodeProperties.nodeFingerTable
        val newNodeProperties = if (rangeValidator(leftInclude = true, n.nodeID, hashNodeRef(nFinger(i).node.get), rightInclude = false, s)) {
          val nFingerBeforeI = nFinger(i).copy(node = Some(sNodeSetup.nodeRef))
          val newFingerTable = nFinger.updated(i, nFingerBeforeI)
          val p = nodeProperties.nodePredecessor.get
          context.log.debug(s"${context.self.path.name} tell Update Finger Table to ${p.path.name}")
          p ! UpdateFingerTable(sNodeSetup, i)
          if (i == 0) {
            nodeProperties.copy(nodeFingerTable = newFingerTable, nodeSuccessor = newFingerTable.head.node)
          }
          else {
            nodeProperties.copy(nodeFingerTable = newFingerTable)
          }

        } else {
          nodeProperties
        }
        context.log.debug(s"Node updated in Last step $newNodeProperties")
        nodeBehaviors(newNodeProperties)
      }

      case GetNodeProperties(replyTo) => {

        val nodePropertiesCopy = nodeProperties.copy()
        replyTo ! ReplyWithNodeProperties(nodePropertiesCopy)
        Behaviors.same
      }
      case PrintUpdate => {
        context.log.info(s"${nodeProperties.toString}")
        Behaviors.same
      }

      case SaveNodeSnapshot(replyTo) => {
        replyTo ! ReplySnapshot(NodeSnapshot(LocalDateTime.now(), nodeProperties))
        Behaviors.same
      }
      case getValues(replyTo: ActorRef[LookupObjects]) =>
        context.log.info(s"My name is ${context.self}")
        replyTo ! LookupObjects(nodeProperties.storedData.values.toSeq)
        Behaviors.same

      case FindNode(requestObject, replyTo) => {
        val node = select_random_node()
        implicit val timeout: Timeout = Timeout(10.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler
        val id = Helper.getIdentifier(requestObject.key)

        val future = node.ask(ref => FindSuccessor(id, ref))
        val resp = Await.result(future, timeout.duration).nSuccessor
        //        context.log.info(s"Found node ${resp.nodeRef.path.name} storing in successor ${resp.nodeSuccessor.get.path} to store $id")
        val future_2 = resp.nodeSuccessor.get.ask(ref => addValue(requestObject, ref))
        val resp1 = Await.result(future_2, timeout.duration)
        context.log.info(resp1.description)
        replyTo ! ActionSuccessful(s"Data Added $resp1")
        Behaviors.same
      }

      case SearchDataNode(key, replyTo) => {
        val node = select_random_node()
        implicit val timeout: Timeout = Timeout(10.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler

        val id = Helper.getIdentifier(key)
        // context.log.info(s"Random node found ${node.path.name}")
        val future = node.ask(ref => FindSuccessor(id, ref))
        val resp = Await.result(future, timeout.duration).nSuccessor

        val future_2 = resp.nodeSuccessor.get.ask(ref => getValue(key, ref))
        val resp1 = Await.result(future_2, timeout.duration).maybeObject.get
        context.log.info(resp1.value)
        replyTo ! ActionSuccessful(s"Data Found $resp1")

        Behaviors.same
      }


      case addValue(requestObject, replyTo) => {
        val update = nodeProperties.copy()
        update.storedData.addOne((Helper.getIdentifier(requestObject.key), requestObject.value))
        nodeBehaviors(nodeProperties.copy(storedData = update.storedData))
        //context.log.info(s"Hashmap for ${nodeProperties.nodeName}  ${nodeProperties.storedData.toSeq.toString()}")
        replyTo ! ActionSuccessful(s"object ${requestObject.key} created at ${nodeProperties.nodeName}")
        Behaviors.same
      }

      case getValue(k, replyTo) =>
        val key = Helper.getIdentifier(URLDecoder.decode(k, "UTF-8"))
        val response = nodeProperties.storedData.get(key)
        if (response.nonEmpty) {
          replyTo ! GetLookupResponse({
            Some(LookupObject(s"Data found at ${context.self.path.name}  value : ${nodeProperties.storedData(key)} "))
          })
        }
        else {

          replyTo ! GetLookupResponse(Some(LookupObject(s"Data Not found ${URLDecoder.decode(k, "UTF-8")}")))
        }
        Behaviors.same

      case _ =>
        context.log.error("Bad Message")
        Behaviors.ignore
    }
    // this function needs to be accessed as a Message or by actor itself.
    // Defining in this location will enable to not waste a message if// node itself is executing this
  }

  private def find_predecessor(id: Int, nodeProperties: NodeSetup): NodeSetup = {

    val n = nodeProperties.copy()
    // Can't help but use var, really. Or, I am dumb
    var nDash = n
    context.log.debug(s"In find predecessor with id : $id n:${nodeProperties.nodeID} ")
    context.log.debug(s"left ${nDash.nodeID} right  ${hashNodeRef(nDash.nodeSuccessor.get)}")

    while (!rangeValidator(leftInclude = false, nDash.nodeID, hashNodeRef(nDash.nodeSuccessor.get), rightInclude = true, id)) {
      context.log.debug(s"left ${nDash.nodeID} right :${hashNodeRef(nDash.nodeSuccessor.get)} id :$id")
      if (nDash.nodeRef.equals(n.nodeRef)) {
        // you are in the same node. No need to call a Future
        val nDashTemp = closest_preceding_finger(id, nodeProperties)
        nDash = nDashTemp
        context.log.debug(s"After ${nDash.nodeName}")
      } else {
        implicit val timeout: Timeout = Timeout(3.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler
        val future: Future[ReplyWithClosestPrecedingFinger] = nDash.nodeRef.ask { ref =>
          ClosestPrecedingFinger(id, ref)
        }
        // Cannot avoid blocking
        context.log.info(s"[${context.self.path.name}] in Find Predecessor. Awaiting result ")
        nDash = Await.result(future, timeout.duration).nodeSetup

        /*
        implicit val ec: ExecutionContextExecutor = context.system.executionContext
        future.onComplete{
          case Success(ReplyWithClosestPrecedingFinger(nodeSetup)) => {nDash = nodeSetup}
          case Failure(exception) => context.log.error(exception.toString)
        }
        */

      }
    }
    context.log.debug(s"[${context.self.path.name}]Sending Reply with Predecessor as ${nDash.toString}")
    nDash
  }

  private def closest_preceding_finger(id: Int,
                                       nodeProperties: NodeSetup): NodeSetup = {
    //TODO check i value
    val nId = nodeProperties.nodeID
    val nFingerTable = nodeProperties.nodeFingerTable
    context.log.debug(s"systemContext: ${SystemConstants.M}")
    for (i <- SystemConstants.M - 1 to 0 by -1) {
      context.log.debug(s" in closest_preceding_finger left :$nId right $id value ${hashFingerTableEntity(nFingerTable(i))} ")
      if (rangeValidator(leftInclude = false, nId, id, rightInclude = false, hashFingerTableEntity(nFingerTable(i)))) {

        val fingerNodeIRef = nFingerTable(i).node.get

        if (fingerNodeIRef.equals(context.self)) {

          // this is purely to eliminate a blocking call (or a deadlock)
          val nDash = nodeProperties.copy()
          return nDash
        }
        else {
          // We know now for sure to ask another node for its copy
          // cannot avoid blocking
          implicit val timeout: Timeout = Timeout(3.seconds)
          implicit val scheduler: Scheduler = context.system.scheduler
          val future: Future[ReplyWithNodeProperties] = fingerNodeIRef.ask(ref => GetNodeProperties(ref))
          val nDash = Await.result(future, timeout.duration).nodeSetup
          return nDash
        }

      }
    }
    nodeProperties.copy()

  }


  // Helper Functions

  private def init_finger_table(nDash: ActorRef[Command],
                                nodeProperties: NodeSetup): NodeSetup = {

    // line1 of algo to find successor
    // and update curr node successor
    val n = context.self
    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val scheduler: Scheduler = context.system.scheduler
    val nFingerTable = nodeProperties.nodeFingerTable
    val future: Future[ReplyWithSuccessor] = nDash.ask(ref => FindSuccessor(nFingerTable.head.start, ref))
    val nDashSuccessorSetup: NodeSetup = Await.result(future, timeout.duration).nSuccessor
    context.log.debug(s"Currently at ${context.self.path.name}, the Sucessor returned is ${nDashSuccessorSetup.nodeName}")
    val newFingerTableHead = nFingerTable.head.copy(node = Some(nDashSuccessorSetup.nodeRef))
    val newSuccessor = newFingerTableHead.node
    // updating predecessor of current node
    val nodePropsOfNewSuccessorFuture: Future[ReplyWithNodeProperties] = newSuccessor.get.ask(ref => GetNodeProperties(ref))
    val nodePropsOfNewSuccessor = Await.result(nodePropsOfNewSuccessorFuture, timeout.duration).nodeSetup
    // successor.predecessor
    val newPredecessor = nodePropsOfNewSuccessor.nodePredecessor
    // successor.predecessor = n
    val future2 = newPredecessor.get.ask(ref => SetPredecessor(n, ref))
    val res = Await.result(future2, timeout.duration).Str
    context.log.debug(s"The node ${newPredecessor.get.path.name} has changed his predecessor. Response was $res")

    // change my fingerTable to reflect the new changes
    val newFTList: ListBuffer[FingerTableEntity] = new ListBuffer[FingerTableEntity]()
    newFTList += newFingerTableHead
    for (i <- 1 until SystemConstants.M) {

      if (rangeValidator(leftInclude = true, hashNodeRef(n), hashFingerTableEntity(newFTList(i - 1)), rightInclude = false, nFingerTable(i).start)) {
        newFTList += nFingerTable(i).copy(node = newFTList(i - 1).node)
      }
      else {
        val future2: Future[ReplyWithSuccessor] = nDash.ask(ref => FindSuccessor(newFTList(i).start, ref))
        val foundSuccessorNodeRef = Await.result(future2, timeout.duration).nSuccessor.nodeRef
        newFTList += newFTList(i).copy(node = Some(foundSuccessorNodeRef))
      }
    }
    // All the node variables that are changed
    //newSuccessor, newPredecessor, newFTList
    val newNodeProperties = nodeProperties.copy(nodePredecessor = newPredecessor, nodeSuccessor = newSuccessor, nodeFingerTable = newFTList.toList)
    newNodeProperties
  }


  private def updateOthers(nodeProperties: NodeSetup): Option[NodeSetup] = {
    context.log.debug(s"UpdateOthers function with ${context.self}")
    val n = nodeProperties.copy()
    var newNodeProperties: Option[NodeSetup] = None
    for (i <- 0 until SystemConstants.M) {
      val idTemp = n.nodeID - Math.pow(2, i).toInt
      val id = if(idTemp < 0) (idTemp + Math.pow(2, SystemConstants.M).toInt) else idTemp
      context.log.debug(s"in update others ${context.self.path.name}  findPredecessor($id) ${n.nodeID}")
      val pNodeSetup = find_predecessor(n.nodeID - Math.pow(2, i).toInt, n)
      context.log.debug(s" print i : $i ")
      val p = pNodeSetup.nodeRef
      context.log.debug(s"Calling update finger table p: ${p.path.name} i:$i  ")
      if(!(p.path.name == n.nodeRef.path.name))
        p ! UpdateFingerTable(n, i)
      else {
        newNodeProperties = Some(nodeProperties.copy())
        newNodeProperties = updateSelfFingerTable(newNodeProperties.get, i)
      }
    }
    newNodeProperties
  }

  private def updateSelfFingerTable(sNodeSetup: NodeSetup,
                                    i: Int,
                                    ): Option[NodeSetup] = {
    context.log.debug(s"In self Update Finger Table of ${context.self.path.name}")
    val s = sNodeSetup.nodeID
    val nodeProperties = sNodeSetup
    val n = nodeProperties
    val nFinger = nodeProperties.nodeFingerTable
    val newNodeProperties = if (rangeValidator(leftInclude = true, n.nodeID, hashNodeRef(nFinger(i).node.get), rightInclude = false, s)) {
      val nFingerBeforeI = nFinger(i).copy(node = Some(sNodeSetup.nodeRef))
      val newFingerTable = nFinger.updated(i, nFingerBeforeI)
      val p = nodeProperties.nodePredecessor.get
      context.log.debug(s"${context.self.path.name} tell Update Finger Table to ${p.path.name}")
      p ! UpdateFingerTable(sNodeSetup, i)
      if (i == 0) {
        nodeProperties.copy(nodeFingerTable = newFingerTable, nodeSuccessor = newFingerTable.head.node)
      }
      else {
        nodeProperties.copy(nodeFingerTable = newFingerTable)
      }
    } else {
      nodeProperties
    }
    Some(newNodeProperties)
  }

  // returns ID of that FingerTable entity
  private def hashFingerTableEntity(f: FingerTableEntity): Int = {
    Helper.getIdentifier(f.node.get.path.name)
  }

  // returns ID of the ActorRef
  private def hashNodeRef(f: ActorRef[Command]): Int = {
    Helper.getIdentifier(f.path.name)
  }


}
