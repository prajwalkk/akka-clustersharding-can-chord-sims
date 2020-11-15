package com.chord.akka.actors

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import com.chord.akka.utils.{Helper, SystemConstants}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

/*
*
* Created by: prajw
* Date: 13-Nov-20
*
*/
object NodeActorTest extends LazyLogging {

  // Protocols
  sealed trait Command
  // ask
  final case class FindSuccessor(id: Int,
                                 replyTo: ActorRef[ReplyWithSuccessor]) extends Command
  // reply
  final case class ReplyWithSuccessor(nSuccessor: NodeSetup) extends Command //TODO

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
  final case class Join(nDash: ActorRef[NodeActorTest.Command]) extends Command
  final case class InitFingerTable(nDash: ActorRef[NodeActorTest.Command]) extends Command

  // tell a node to update its Predecessor to the passed node
  final case class SetPredecessor(nodeRef: ActorRef[NodeActorTest.Command]) extends Command

  final case object UpdateOthers extends Command
  // update all nodes
  final case class UpdateFingerTable(s: ActorRef[NodeActorTest.Command],
                                     i: ActorRef[NodeActorTest.Command]) extends Command

  // Use this instead of passing way too many parameters
  case class NodeSetup(nodeName: String,
                               nodeID: Int,
                               nodeRef: ActorRef[NodeActorTest.Command],
                               nodeSuccessor: Option[ActorRef[NodeActorTest.Command]],
                               nodePredecessor: Option[ActorRef[NodeActorTest.Command]],
                               nodeFingerTable: List[FingerTableEntity2]
                              // TODO add data storage
                              )

  def apply(nodeName: String): Behavior[Command] = {
    // Each node has a:
    // Node ID
    // Node Name
    // Node Successor
    // Node Predecessor
    // Fingertable

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
        nodeFingerTable = nodeFingerTable)
      new NodeActorTest(nodeName, context).nodeBehaviors(nodePropertiesWhenCreated)
    }

  }

  // This is just to set up a node when it is created the first time
  private def getTemplateFingerTable(id: Int): List[FingerTableEntity2] = {
    val m = SystemConstants.M
    val fingerTableList = (1 to m).map { k =>
      val startVal: Int = (id + Math.pow(2, k - 1).toInt) % Math.pow(2, m).toInt
      val endVal: Int = if (k == m) id else (id + Math.pow(2, k).toInt) % Math.pow(2, m).toInt
      FingerTableEntity2(start = startVal, startInterval = startVal, endInterval = endVal, node = None)
    }
    fingerTableList.toList
  }

}



class NodeActorTest private(name: String,
                            context: ActorContext[NodeActorTest.Command]) {

  import NodeActorTest._


  private def nodeBehaviors(nodeProperties: NodeSetup): Behavior[Command] = {
    // We already have access to context. Hence
    Behaviors.receiveMessage {
      case Join(nDash) => {
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
          context.log.debug(s"[$name]Update node Properties: ${newSetup.toString}")
          nodeBehaviors(newSetup)
        } else {
          context.log.info("Different node calling node 0")
          // if it is a different node
          // n ! Join(nDash)
          // n ! InitFingerTable(nDash)
          context.log.debug(s"[${context.self.path.name}] Init FingerTable Old props: ${nodeProperties.toString}")
          val newNodeProperties = init_finger_table(nDash, nodeProperties)
          context.log.debug(s"[${context.self.path.name}] Init FingerTable: ${newNodeProperties.toString} ")
          nodeBehaviors(newNodeProperties)
          // TODO convert to a function
          Behaviors.same
        }
      }

      case FindSuccessor(idActorRef, replyTo) => {
        val nDash = find_predecessor(idActorRef, nodeProperties.copy())
        val nDashRef = nDash.nodeRef
        implicit val timeout: Timeout = Timeout(3.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler
        val future: Future[ReplyWithNodeProperties] = nDashRef.ask(ref => GetNodeProperties(ref))
        val nDashSuccessor = Await.result(future, timeout.duration).nodeSetup
        replyTo ! ReplyWithSuccessor(nDashSuccessor)
        Behaviors.same
      }

      case FindPredecessor(idActorRef, replyTo: ActorRef[NodeActorTest.Command]) => {
        context.log.debug(s"[${context.self.path.name}] Received a find Predecessor request from ${replyTo.path.name}")
        val nDash = find_predecessor(idActorRef, nodeProperties)
        replyTo ! ReplyWithPredecessor(nDash.copy())
        context.log.debug(s"[${context.self.path.name}] in Find Predecessor. Got result and replied to: ${replyTo.path.name} ")
        Behaviors.same
      }

      case ClosestPrecedingFinger(id, replyTo) => {
        context.log.debug(s"[${context.self.path.name}] Executing closest preceding finger. Replyto: ${replyTo.path.name} ")
        val n = closest_preceding_finger(id, nodeProperties)
        replyTo ! ReplyWithClosestPrecedingFinger(n)
        context.log.debug(s"[${context.self.path.name}] Executed n = ${n.toString}. Reply done: ${replyTo.path.name} ")
        Behaviors.same
      }

      case InitFingerTable(nDash) => {
        context.log.debug(s"[${context.self.path.name}] Init FingerTable Old props: ${nodeProperties.toString}")
        val newNodeProperties = init_finger_table(nDash, nodeProperties)
        context.log.debug(s"[${context.self.path.name}] Init FingerTable: ${newNodeProperties.toString} ")
        nodeBehaviors(newNodeProperties)
      }
      case SetPredecessor(nodeRef) => {
        context.log.debug(s"[${context.self.path.name}] Setting my predecessor to ${nodeRef.path.name}")
        val newPredecessor = nodeRef
        val newNodeProps = nodeProperties.copy(nodePredecessor = Some(newPredecessor))
        context.log.debug(s"[${context.self.path.name}] New Props to ${newNodeProps.toString}")
        nodeBehaviors(newNodeProps)
      }

      case UpdateOthers => Behaviors.same

      case UpdateFingerTable(s, i) => Behaviors.same

      case GetNodeProperties(replyTo) => {

        val nodePropertiesCopy = nodeProperties.copy()
        replyTo ! ReplyWithNodeProperties(nodePropertiesCopy)
        Behaviors.same
      }
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

    while (!isIdentifierInInterval(id, Array(nDash.nodeID, hashNodeRef(nDash.nodeSuccessor.get)), '(')) {
      if ((nDash.nodeRef).equals(n.nodeRef)) {
        // you are in the same node. No need to call a Future
        closest_preceding_finger(id, nodeProperties)
      } else {
        implicit val timeout: Timeout = Timeout(3.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler
        val future: Future[ReplyWithClosestPrecedingFinger] = nDash.nodeRef.ask { ref =>
          ClosestPrecedingFinger(id, ref)
        }
        // Cannot avoid blocking
        context.log.debug(s"[${context.self.path.name}] in Find Predecessor. Awaiting result ")
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
    context.log.debug(s"[${context.self.path.name}]Sending Reply with Predcecessor as ${nDash.toString}")
    nDash
  }

  private def closest_preceding_finger(id: Int,
                                       nodeProperties: NodeSetup): NodeSetup = {

    val nId = nodeProperties.nodeID
    val nFingerTable = nodeProperties.nodeFingerTable
    for (i <- SystemConstants.M to 1)
    if (isIdentifierInInterval(hashFingerTableEntity(nFingerTable(i)), Array(nId, id))) {
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
    nodeProperties.copy()

  }

  private def init_finger_table(nDash: ActorRef[Command],
                                nodeProperties: NodeSetup): NodeSetup ={

    // line1 of algo to find successor
    // and update curr node successor
    val n = context.self
    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val scheduler: Scheduler = context.system.scheduler
    val nFingerTable = nodeProperties.nodeFingerTable
    val future: Future[ReplyWithSuccessor] = nDash.ask(ref => FindSuccessor(nFingerTable.head.start, ref))
    val nDashSuccessorSetup: NodeSetup = Await.result(future, timeout.duration).nSuccessor
    val newFingerTableHead = nFingerTable.head.copy(node = Some(nDashSuccessorSetup.nodeRef))
    val newSuccessor = newFingerTableHead.node
    // updating predecessor of current node
    val nodePropsOfNewSuccessorFuture: Future[ReplyWithNodeProperties] = newSuccessor.get.ask(ref => GetNodeProperties(ref))
    val nodePropsOfNewSuccessor = Await.result(nodePropsOfNewSuccessorFuture, timeout.duration).nodeSetup
    val newPredecessor = nodePropsOfNewSuccessor.nodePredecessor
    newPredecessor.get ! SetPredecessor(n)

    // change my fingerTable to reflect the new changes
    val newFTList: ListBuffer[FingerTableEntity2] = new ListBuffer[FingerTableEntity2]()
    newFTList += newFingerTableHead
    for(i <- 1 until SystemConstants.M){
      if(isIdentifierInInterval(nFingerTable(i).start, Array(hashNodeRef(n), hashFingerTableEntity(nFingerTable(i - 1))))){
        newFTList += nFingerTable(i).copy(node = nFingerTable(i - 1).node)
      }
      else{
        val future2: Future[ReplyWithSuccessor] = nDash.ask(ref => FindSuccessor(nFingerTable(i).start, ref))
        val foundSuccessorNodeRef = Await.result(future2, timeout.duration).nSuccessor.nodeRef
        newFTList += nFingerTable(i).copy(node = Some(foundSuccessorNodeRef))
      }
    }
    // All the node variables that are changed
    //newSuccessor, newPredecessor, newFTList
    val newNodeProperties = nodeProperties.copy(nodePredecessor = newPredecessor,nodeSuccessor = newSuccessor, nodeFingerTable = newFTList.toList)
    newNodeProperties
  }


  // Helper Functions

  // returns ID of that fingertable entity
  private def hashFingerTableEntity(f: FingerTableEntity2): Int = {
    Helper.getIdentifier(f.node.get.path.name)
  }

  // returns ID of the actroref
  private def hashNodeRef(f: ActorRef[Command]): Int = {
    Helper.getIdentifier(f.path.name)
  }

  private def isIdentifierInInterval(identifier: Int, interval: Array[Int], equality: Char = 'b'): Boolean = {
    val bitSize = SystemConstants.M
    equality match {
      // check (a,b]
      case '(' => {
        if (interval(0) < interval(1)) {
          if (identifier > interval(0) && identifier <= interval(1))
            return true
          else
            return false
        }
        val interval1: Array[Int] = Array(interval(0), Math.pow(2, bitSize).asInstanceOf[Int])
        val interval2: Array[Int] = Array(0, interval(1))
        isIdentifierInInterval(identifier, interval1, '<') || isIdentifierInInterval(identifier, interval2, '<')
      }
      case ')' => {
        // check [a, b)
        if (interval(0) < interval(1)) {
          if (identifier >= interval(0) && identifier < interval(1)) {

            return true
          } else {

            return false
          }
        }
        val interval1: Array[Int] = Array(interval(0), Math.pow(2, bitSize).asInstanceOf[Int])
        val interval2: Array[Int] = Array(0, interval(1))
        isIdentifierInInterval(identifier, interval1, ')') || isIdentifierInInterval(identifier, interval2, ')')
      }
      case _ => {
        // check [a, b]
        if (interval(0) < interval(1)) {
          if (identifier > interval(0) && identifier < interval(1))
            return true
          else
            return false
        }
        val interval1: Array[Int] = Array(interval(0), Math.pow(2, bitSize).asInstanceOf[Int])
        val interval2: Array[Int] = Array(0, interval(1))
        isIdentifierInInterval(identifier, interval1) || isIdentifierInInterval(identifier, interval2)
      }
    }
  }



}
