package com.can.akka.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.can.akka.actors.NodeGroup.NodeList
import com.can.akka.simulation.Simulation.{select_random_node, sharding, typekey}
import com.can.akka.utils.{Coordinate, Helper, Neighbour, SystemConstants}
import com.typesafe.scalalogging.LazyLogging

import java.net.URLDecoder
import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object NodeActor extends LazyLogging {


  def apply(nodeName: String): Behavior[Command] = {
    Behaviors.setup { context =>
      val neighbours: mutable.HashSet[Neighbour] = new mutable.HashSet[Neighbour]().empty
      val nodeRef = sharding.entityRefFor(typekey, context.self.path.name)
      val nodeProperties = CanNodeSetup(
        nodeName = nodeName,
        nodeRef = nodeRef,
        coordinates = None,
        neighbours = neighbours,
        storedData = mutable.HashMap.empty,
        zoneSplitOnx = false
      )
      new NodeActor(nodeName, context).nodeBehavioursCAN(nodeProperties)
    }
  }

  // Protocols
  sealed trait Command

  case class FailNode() extends Command

  case class CanNodeSetup(nodeName: String,
                          nodeRef: EntityRef[NodeActor.Command],
                          coordinates: Option[Coordinate],
                          neighbours: mutable.HashSet[Neighbour],
                          storedData: mutable.HashMap[datakey, String],
                          zoneSplitOnx: Boolean
                         ) {
    override def toString: String = s"\nNodeName :$nodeName\n, Path = ${nodeRef}\n, Coordinates = ${coordinates.toString} \n, Neighbours = ${neighbours.toSeq.toString()}"
  }


  final case class LeaveNode() extends Command

  final case class Merge(newdata: mutable.HashMap[datakey, String], newneighbours: mutable.HashSet[Neighbour], newzone: Coordinate) extends Command

  final case class UpdateData(replyTo: ActorRef[replyUpdateData]) extends Command

  final case class replyUpdateData(data: mutable.HashMap[datakey, String])

  final case class getStatus(replyTo: ActorRef[ActionSuccessful]) extends Command

  final case class replyStatus(nodeSetup: CanNodeSetup)

  final case class getNodeStatus(replyTo: ActorRef[ActionSuccessful]) extends Command

  final case class CAN_Join(existingNode: EntityRef[NodeActor.Command], replyTo: ActorRef[ActionSuccessful]) extends Command

  case class ActionSuccessful(description: String)

  final case class FindZone(x: Double, y: Double, newnode: EntityRef[NodeActor.Command], replyTo: ActorRef[FindZoneReply]) extends Command

  final case class FindZoneReply(node: EntityRef[NodeActor.Command], status: String, hops: Int)

  final case class SplitZone(x: Double, y: Double, newnode: EntityRef[NodeActor.Command], replyTo: ActorRef[ReplySplitZone]) extends Command

  final case class ReplySplitZone(coordinates: Coordinate, neighbour_list: mutable.HashSet[Neighbour])

  final case class RemoveNeighbour(coordinate: Coordinate, node: EntityRef[NodeActor.Command]) extends Command

  final case class UpdateNeighbour(coordinate: Coordinate, node: EntityRef[NodeActor.Command]) extends Command

  final case class FindNode(requestObject: RequestObject, replyTo: ActorRef[ActionSuccessful]) extends Command

  final case class SearchDataNode(lookup: String, replyTo: ActorRef[ActionSuccessful]) extends Command

  final case class getValues(replyTo: ActorRef[String]) extends Command

  final case class addValue(requestObject: RequestObject, replyTo: ActorRef[ActionSuccessful]) extends Command

  final case class getValue(k: String, replyTo: ActorRef[GetLookupResponse]) extends Command

  case class GetLookupResponse(maybeObject: Option[LookupObject])


}

class NodeActor(name: String, context: ActorContext[NodeActor.Command]) {

  import NodeActor._

  def isTouchingHorizontally(curr_zone: Coordinate, neighbour_zone: Coordinate): Boolean = {
    if (neighbour_zone.leftX == curr_zone.rightX || neighbour_zone.rightX == curr_zone.leftX)
      return true
    false

  }

  def isSubsetY(curr_zone: Coordinate, neighbour_zone: Coordinate): Boolean = {

    if ((neighbour_zone.bottomY >= curr_zone.bottomY && neighbour_zone.topY <= curr_zone.topY) || (curr_zone.bottomY >= neighbour_zone.bottomY && curr_zone.topY <= neighbour_zone.topY))
      return true

    false
  }

  def isSubsetX(curr_zone: Coordinate, coordinate: Coordinate): Boolean = {

    if ((curr_zone.leftX <= coordinate.leftX && curr_zone.rightX >= coordinate.rightX) || (coordinate.leftX <= curr_zone.leftX && coordinate.rightX >= curr_zone.rightX))
      return true

    false
  }

  private def nodeBehavioursCAN(currentNodeProperties: CanNodeSetup): Behavior[Command] = {

    Behaviors.receiveMessage {

      case getNodeStatus(replyto) => {
        var result = ""
        implicit val timeout: Timeout = Timeout(10.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler
        NodeList.foreach(node => {
          val future = getEntity(node).ask(ref => getStatus(ref))
          val res = Await.result(future, timeout.duration).description
          result = result + res
        })

        replyto ! ActionSuccessful(result)
        Behaviors.same
      }
      case getStatus(replyTo) => {
        context.log.info(s"[getStatus] ${currentNodeProperties.toString}")
        replyTo ! ActionSuccessful(s"${currentNodeProperties.toString}")
        Behaviors.same
      }
      case CAN_Join(existingNode, replyTo) => {
        var newprops = currentNodeProperties.copy()
        var nodeToSplit: Option[EntityRef[NodeActor.Command]] = None
        implicit val timeout: Timeout = Timeout(3.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler
        if (getEntity(context.self.path.name).toString.equals((existingNode).toString)) {
          context.log.info(s"[CAN_Join] CAN Zone created with coordinates (${SystemConstants.X1},${SystemConstants.Y1}) and (${SystemConstants.X2},${SystemConstants.Y2})")
          context.log.info(s"[CAN_Join] First Node Joining ${context.self.path.name}")
          val newCoordinates = Coordinate(SystemConstants.X1, SystemConstants.Y1, SystemConstants.X2, SystemConstants.Y2)
          context.log.info(s"[CAN_Join] ${context.self.path.name} assigned to zone with coordinates (${SystemConstants.X1},${SystemConstants.Y1}) and (${SystemConstants.X2},${SystemConstants.Y2})")
          newprops = currentNodeProperties.copy(coordinates = Some(newCoordinates))
        }
        else {
          context.log.info(s"[CAN_Join] Node Joining ${context.self.path.name} with ${existingNode}")
          //generate Random x,y for new node
          val x = Helper.getRandomCoordinate()
          val y = Helper.getRandomCoordinate()

          context.log.debug(s"[CAN_Join] Finding node Zone in which the ($x,$y) lie")
          var status = "Fail"
          var check_node = existingNode
          while (status == "Fail") {
            val future: Future[FindZoneReply] = check_node.ask(ref => FindZone(x, y, getEntity(context.self.path.name), ref))
            val result_temp = Await.result(future, timeout.duration)
            context.log.debug(s"${result_temp.status}")
            check_node = result_temp.node
            status = result_temp.status
          }
          nodeToSplit = Some(check_node)
          context.log.debug(s"[CAN_Join] Found the node ${nodeToSplit.get} whose zone contains ($x,$y)")
          context.log.info(s"[CAN_Join] splitting ${nodeToSplit.get} zone to accommodate ${context.self.path.name} Node")
          val future_split: Future[ReplySplitZone] = nodeToSplit.get.ask(ref => SplitZone(x, y, getEntity(context.self.path.name), ref))
          val result = Await.result(future_split, timeout.duration)
          val newZone = result.coordinates
          val newneighbours = result.neighbour_list
          newprops = currentNodeProperties.copy(coordinates = Some(newZone), neighbours = newneighbours)
          if (nodeToSplit.isDefined) {
            context.log.info(s"Checking data of the split node ${nodeToSplit.get} to transfer to new node ")
            val future = nodeToSplit.get.ask(ref => UpdateData(ref))
            val data_to_insert_in_newnode = Await.result(future, timeout.duration).data
            val data_temp = newprops.storedData ++ data_to_insert_in_newnode
            newprops = newprops.copy(storedData = data_temp)
            context.log.info(s"[CAN_Join] ${currentNodeProperties.nodeName} Updated after Join ${newprops.toString}")
          }
        }
        replyTo ! ActionSuccessful(s"${currentNodeProperties.nodeRef} joined ")
        nodeBehavioursCAN(newprops)
      }

      case LeaveNode() => {
        context.log.info(s"[LeaveNode] Failing ${context.self.path.name} Node")
        val findmerge_node = findMergeNode(currentNodeProperties)
        if (findmerge_node.isDefined) {
          val merge_neighbour = findmerge_node.get
          context.log.info(s"[LeaveNode] Merging ${context.self.path.name} Node zone with ${merge_neighbour.node} Node")
          val new_node_data = currentNodeProperties.storedData
          //New Neighbours to add to merge node (Removing itself from the list)
          val new_neighbours_append = currentNodeProperties.neighbours
          new_neighbours_append.remove(merge_neighbour)
          context.log.info(s"[LeaveNode] All neighbours of ${context.self.path.name} removed ${context.self.path.name} from their neighbours")
          context.log.info(s"[LeaveNode] Transferring all data from ${context.self.path.name} to ${merge_neighbour.node}")
          context.log.info(s"[LeaveNode] Transferring all neighbours from ${context.self.path.name} to ${merge_neighbour.node}")
          val merge_node_zone_updated = mergeCoordinate(currentNodeProperties.coordinates.get, merge_neighbour.coordinate)
          merge_neighbour.node ! Merge(new_node_data, new_neighbours_append, merge_node_zone_updated)
          NodeList = NodeList.filter(_ != currentNodeProperties.nodeRef)
        }
        else {
          //no node found to merge throw an exception and resume the actor in Behaviours Supervise Strategy
          context.log.info(s"[LeaveNode] This Node cannot fail no suitable merge nodes found Restarting")
        }
        Behaviors.same
      }
      case Merge(newdata, newneighbours, newzone) => {
        val old_data = currentNodeProperties.storedData
        val updated_data = old_data ++ newdata
        val old_neighbours = currentNodeProperties.neighbours
        val updated_neighbours = old_neighbours ++ newneighbours
        val newprops = currentNodeProperties.copy(coordinates = Some(newzone), neighbours = updated_neighbours, storedData = updated_data)
        //Updating merge zone of the node in its neighbours
        updated_neighbours.foreach(neighbour => {
          neighbour.node ! UpdateNeighbour(newzone, currentNodeProperties.nodeRef)
          Thread.sleep(100)
        })
        Thread.sleep(1000)
        context.log.info(s"[Merge] Node ${currentNodeProperties.nodeRef} after merge ${newprops.toString}")
        nodeBehavioursCAN(newprops)
      }

      case UpdateData(replyTo) => {
        val curr_zone = currentNodeProperties.coordinates
        val data = currentNodeProperties.storedData
        val data_to_remove = mutable.HashMap[datakey, String]().empty
        data.foreach(entry => {
          if (!PointInZone(entry._1.x, entry._1.y, curr_zone.get)) {
            data_to_remove.addOne(entry)
            data.remove(entry._1)
          }
        })
        if (data_to_remove.nonEmpty) {
          context.log.info(s"Transferring ${data_to_remove.size} records from ${context.self.path.name}")
        }
        else {
          context.log.info(s"No Data Transfer Required from ${context.self.path.name}")
        }
        val newprops = currentNodeProperties.copy(storedData = data)
        replyTo ! replyUpdateData(data_to_remove)
        nodeBehavioursCAN(newprops)
      }
      case RemoveNeighbour(coordinate, node) => {
        context.log.info(s"[RemoveNeighbour] Removing ${node} as neighbour to ${context.self.path.name}")
        val old_neighbour = currentNodeProperties.neighbours.filter(_.node == node)
        var new_neighbours = currentNodeProperties.neighbours
        new_neighbours = new_neighbours -- old_neighbour.iterator
        val newprops = currentNodeProperties.copy(neighbours = new_neighbours)
        context.log.info(s"[RemoveNeighbour] updated ${context.self.path.name} ${newprops.toString}")
        nodeBehavioursCAN(newprops)
      }
      case UpdateNeighbour(coordinate, node) => {
        context.log.info(s"[UpdateNeighbour] Updating ${node} zone in ${context.self.path.name}")
        val old_neighbour = currentNodeProperties.neighbours.filter(_.node == node)
        var new_neighbours = currentNodeProperties.neighbours
        new_neighbours = new_neighbours -- old_neighbour.iterator
        new_neighbours.add(Neighbour(coordinate, node))
        val newprops = currentNodeProperties.copy(neighbours = new_neighbours)
        context.log.info(s"[UpdateNeighbour] updated ${context.self.path.name} ${newprops.toString}")
        nodeBehavioursCAN(newprops)
      }

      case SplitZone(x, y, newnode, replyTo) => {
        // Check is zone is split on x axis earlier
        val currNodeleftx = currentNodeProperties.coordinates.get.leftX
        val currNodebottomy = currentNodeProperties.coordinates.get.bottomY
        var currNoderightx = currentNodeProperties.coordinates.get.rightX
        var currNodetopy = currentNodeProperties.coordinates.get.topY
        var zoneSplitOnx_update = currentNodeProperties.zoneSplitOnx

        var newnodeZone: Option[Coordinate] = None


        if (!currentNodeProperties.zoneSplitOnx) {
          //vertical split
          context.log.info(s"[SplitZone] Splitting ${context.self.path.name} Zone vertically ")
          val newleftX = (currentNodeProperties.coordinates.get.leftX + currentNodeProperties.coordinates.get.rightX) / 2
          val newbottomY = currentNodeProperties.coordinates.get.bottomY
          val newrightX = (currentNodeProperties.coordinates.get.rightX)
          val newtopY = (currentNodeProperties.coordinates.get.topY)
          currNoderightx = newleftX
          newnodeZone = Some(Coordinate(newleftX, newbottomY, newrightX, newtopY))
          zoneSplitOnx_update = true
        }
        else {
          //Horizontal Split
          context.log.info(s"[SplitZone] Splitting ${context.self.path.name} Zone Horizontally ")
          val newleftX = currentNodeProperties.coordinates.get.leftX
          val newbottomY = (currentNodeProperties.coordinates.get.bottomY + currentNodeProperties.coordinates.get.topY) / 2
          val newrightX = (currentNodeProperties.coordinates.get.rightX)
          val newtopY = (currentNodeProperties.coordinates.get.topY)
          currNodetopy = newbottomY
          newnodeZone = Some(Coordinate(newleftX, newbottomY, newrightX, newtopY))
          zoneSplitOnx_update = false
        }

        val currentNodeUpdatedCoordinates = Coordinate(currNodeleftx, currNodebottomy, currNoderightx, currNodetopy)
        var current_neighbours = currentNodeProperties.neighbours
        var newnode_neighbours = mutable.HashSet[Neighbour]().empty
        //Removing the nodes which are no longer neighbours of current node after split and checking which neighbours can be added to new node
        val neighboursCheck = checkNeighbourstoRemove(current_neighbours, currentNodeUpdatedCoordinates, currentNodeProperties.nodeRef, newnodeZone.get)
        val neighbours_to_remove_from_current = neighboursCheck(0)
        val neighbours_to_add_from_current_to_newNode = neighboursCheck(1)
        context.log.debug(s"Returned from function checkNeighbourstoRemove ${neighbours_to_remove_from_current.toSeq}")
        current_neighbours = current_neighbours -- neighbours_to_remove_from_current.iterator
        //Adding the nodes as neighbours to new node which are removed from current node after split(this means vertical split)
        context.log.debug(s"[SplitZone] Adding ${currentNodeProperties.nodeRef}'s subset of neighbours to ${newnode}'")
        newnode_neighbours.addAll(neighbours_to_remove_from_current)
        newnode_neighbours.addAll(neighbours_to_add_from_current_to_newNode)
        //Nodes that were added as neighbours to newnode must add newnode as their neighbour
        if (neighbours_to_add_from_current_to_newNode.nonEmpty) {
          neighbours_to_add_from_current_to_newNode.foreach(neighbour => {
            neighbour.node ! UpdateNeighbour(newnodeZone.get, newnode)
          })
        }
        if (neighbours_to_remove_from_current.nonEmpty) {
          neighbours_to_remove_from_current.foreach(neighbour => {
            neighbour.node ! UpdateNeighbour(newnodeZone.get, newnode)
          })
        }
        //Current node has no neighbours then set them as each others Neighbours
        current_neighbours.add(Neighbour(newnodeZone.get, newnode))
        newnode_neighbours.add(Neighbour(currentNodeUpdatedCoordinates, getEntity(context.self.path.name)))
        replyTo ! ReplySplitZone(newnodeZone.get, newnode_neighbours)
        val currentNodeUpdatedProperties = currentNodeProperties.copy(coordinates = Some(currentNodeUpdatedCoordinates), zoneSplitOnx = zoneSplitOnx_update, neighbours = current_neighbours)
        context.log.info(s"[SplitZone] ${currentNodeProperties.nodeName} Coordinates Updated after Split ${currentNodeUpdatedProperties.toString}")
        nodeBehavioursCAN(currentNodeUpdatedProperties)
      }

      case FindZone(x, y, newnode, replyTo) => {
        var hops = 0
        val inCurrentZone = PointInZone(x, y, currentNodeProperties.coordinates.get)
        // context.log.info(s" $x , $y in ${currentNodeProperties.coordinates.get} $inCurrentZone")
        var targetNode: EntityRef[NodeActor.Command] = currentNodeProperties.nodeRef
        var status: Option[String] = None
        if (inCurrentZone) {
          targetNode = currentNodeProperties.nodeRef
          status = Some("Success")
        } else {

          context.log.debug(s"[FindZone]Getting Closest Neighbour from ${context.self.path.name} for the Point $x,$y")
          val shortest_node_Neighbour = getClosestNeighbour(x, y, currentNodeProperties.neighbours)
          hops = hops + 1
          targetNode = shortest_node_Neighbour.node
          if (PointInZone(x, y, shortest_node_Neighbour.coordinate))
            status = Some("Success")
          else
            status = Some("Fail")
        }
        replyTo ! FindZoneReply(targetNode, status.get, hops)
        Behaviors.same
      }
      case FindNode(requestObject, replyTo) => {
        val random_node = select_random_node()
        val x = Helper.getX(URLDecoder.decode(requestObject.key, "UTF-8"))
        val y = Helper.getY(URLDecoder.decode(requestObject.key, "UTF-8"))
        //context.log.debug(s"Hashed key $x $y")
        var status = "Fail"
        var check_node = random_node
        implicit val timeout: Timeout = Timeout(10.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler
        context.log.debug(s"[FindNode] Finding node Zone in which the ($x,$y) lie")
        var hops = 0
        while (status == "Fail") {
          val future: Future[FindZoneReply] = check_node.ask(ref => FindZone(x, y, getEntity(context.self.path.name), ref))
          val result_temp = Await.result(future, timeout.duration)
          context.log.debug(s"${result_temp.status}")
          check_node = result_temp.node
          status = result_temp.status
          hops = hops + 1 + result_temp.hops
        }
        val target_node = check_node
        context.log.debug(s"[FindNode] Found the node ${target_node} whose zone contains ($x,$y)")
        context.log.info(s"[FindNode] Inserting data with key ($x,$y) at ${target_node} node")
        val future: Future[ActionSuccessful] = target_node.ask(ref => addValue(requestObject, ref))
        val result = Await.result(future, timeout.duration).description
        context.log.info(s"[FindNode] $result in hops :$hops")
        replyTo ! ActionSuccessful(s"$result in hops :$hops")
        Behaviors.same
      }
      case SearchDataNode(k, replyTo) => {
        val random_node = select_random_node()
        val x = Helper.getX(URLDecoder.decode(k, "UTF-8"))
        val y = Helper.getY(URLDecoder.decode(k, "UTF-8"))
        var status = "Fail"
        var check_node = random_node
        implicit val timeout: Timeout = Timeout(3.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler

        context.log.info(s"[SearchDataNode] Finding node Zone in which the ($x,$y) lie")
        var hops = 0
        while (status == "Fail") {
          val future: Future[FindZoneReply] = check_node.ask(ref => FindZone(x, y, getEntity(context.self.path.name), ref))
          val result_temp = Await.result(future, timeout.duration)
          check_node = result_temp.node
          status = result_temp.status
          hops = hops + 1 + result_temp.hops
        }
        val target_node = check_node
        context.log.debug(s"[SearchDataNode] Found the node ${target_node} whose zone contains ($x,$y)")
        context.log.info(s"[SearchDataNode] Looking up data with key ($x,$y) at ${target_node} node")
        val future: Future[GetLookupResponse] = target_node.ask(ref => getValue(URLDecoder.decode(k, "UTF-8"), ref))
        val result = Await.result(future, timeout.duration)
        context.log.info(s"$result in hops :$hops")
        replyTo ! ActionSuccessful(s"$result in hops :$hops")
        Behaviors.same

      }
      case addValue(requestObject, replyTo) => {
        val key = (URLDecoder.decode(requestObject.key, "UTF-8"))
        val x = Helper.getX(key)
        val y = Helper.getY(key)
        val coordinate = datakey(x, y)
        val data = currentNodeProperties.storedData
        // context.log.info(s"[addValue] initial data $data")
        data.addOne(coordinate, requestObject.value)
        //context.log.info(s"[addValue] updated data $data")
        val newprops = currentNodeProperties.copy(storedData = data)
        replyTo ! ActionSuccessful(s"[addValue]Data inserted at ${currentNodeProperties.nodeRef} with key ${datakey} and value ${requestObject.value}")
        nodeBehavioursCAN(newprops)
      }

      case getValue(k, replyTo) => {
        val key = (URLDecoder.decode(k, "UTF-8"))
        val x = Helper.getX(key)
        val y = Helper.getY(key)
        val coordinate = datakey(x, y)
        val data = currentNodeProperties.storedData
        val response = data.get(coordinate)
        if (response.nonEmpty) {
          replyTo ! GetLookupResponse({
            Some(LookupObject(s"[getValue]  Data found at ${context.self.path.name} key: $key ; value : ${response.get} "))
          })
        }
        else {

          replyTo ! GetLookupResponse(Some(LookupObject(s"[getValue]  Data Not found for $key at ${context.self.path.name} ")))
        }
        Behaviors.same
      }
    }
  }

  private def findMergeNode(nodeSetup: CanNodeSetup): Option[Neighbour] = {

    val current_neighbours = nodeSetup.neighbours
    val possible_merge_neighbours = mutable.HashSet[Neighbour]().empty
    var merge_node: Option[Neighbour] = None
    current_neighbours.foreach(neighbour => {
      if (isSameSize(nodeSetup.coordinates.get, neighbour.coordinate)) {
        possible_merge_neighbours.add(neighbour)
      }
    })

    //if there are multiple neighbours that can merge choose one closest to the leaving zone
    if (possible_merge_neighbours.nonEmpty) {
      //tell all neighbours of leaving node to remove the node from their neighbours
      current_neighbours.foreach(neighbour => {
        neighbour.node ! RemoveNeighbour(nodeSetup.coordinates.get, nodeSetup.nodeRef)
        Thread.sleep(100)
      })
      val centerX = (nodeSetup.coordinates.get.leftX + nodeSetup.coordinates.get.rightX) / 2
      val centerY = (nodeSetup.coordinates.get.bottomY + nodeSetup.coordinates.get.topY) / 2
      merge_node = Some(getClosestNeighbour(centerX, centerY, possible_merge_neighbours))
    }

    if (possible_merge_neighbours.size == 1) {
      merge_node = Some(possible_merge_neighbours.head)
    }

    merge_node
  }

  private def isSameSize(leave_node_coordinate: Coordinate, neighbour_coordinate: Coordinate): Boolean = {

    if (((leave_node_coordinate.rightX - leave_node_coordinate.leftX) == (neighbour_coordinate.rightX - neighbour_coordinate.leftX)) && ((leave_node_coordinate.topY - leave_node_coordinate.bottomY) == (neighbour_coordinate.topY - neighbour_coordinate.bottomY)))
      return true
    false


  }

  private def mergeCoordinate(leave_node_coordinate: Coordinate, neighbour_coordinate: Coordinate): Coordinate = {
    val leftx = leave_node_coordinate.leftX.min(neighbour_coordinate.leftX)
    val bottomy = leave_node_coordinate.bottomY.min(neighbour_coordinate.bottomY)
    val rightx = leave_node_coordinate.rightX.max(neighbour_coordinate.rightX)
    val topy = leave_node_coordinate.topY.max(neighbour_coordinate.topY)
    Coordinate(leftx, bottomy, rightx, topy)
  }

  private def getClosestNeighbour(x: Double, y: Double, neighbours: mutable.HashSet[Neighbour]): Neighbour = {
    //context.log.debug(s"[getClosestNeighbour] $neighbours")
    // might fail some times tends to oscillate between 2 points is target point is closer to curr_neighbour than its zone
    var shortestDistance: Double = Integer.MAX_VALUE
    var shortest_neighbour_node: Option[Neighbour] = None
    var pointinzone = false
    neighbours.foreach(neighbour => {
      val centerX = (neighbour.coordinate.leftX + neighbour.coordinate.rightX) / 2
      val centerY = (neighbour.coordinate.bottomY + neighbour.coordinate.topY) / 2
      val distance = math.sqrt(math.pow(centerX - x, 2) + math.pow(centerY - y, 2))
      // made change here to test fix for failure adding if(PointInZone) and added flag to return node directly without checking distance further
      if (PointInZone(x, y, neighbour.coordinate)) {
        shortest_neighbour_node = Some(neighbour)
        pointinzone = true
      }
      else if (distance < shortestDistance && !pointinzone) {
        shortestDistance = distance
        shortest_neighbour_node = Some(neighbour)

      }
    })
    shortest_neighbour_node.get
  }

  private def checkNeighbourstoRemove(neighbours: mutable.HashSet[Neighbour], currentZone: Coordinate, curr_node: EntityRef[NodeActor.Command], newnodeZone: Coordinate): List[mutable.HashSet[Neighbour]] = {
    //check which neighbours to remove after split and notify them to remove current node as their neighbour
    context.log.debug(s"[checkNeighbourstoRemove] checking neighbours to remove from ${curr_node} after split")
    val neighbour_remove = mutable.HashSet[Neighbour]()
    val neighbours_to_add_to_newNode = mutable.HashSet[Neighbour]()
    neighbours.foreach(neighbour => {
      if (!isNeighbour(currentZone, neighbour.coordinate)) {
        neighbour_remove.add(neighbour)
        context.log.info(s"[checkNeighbourstoRemove] removing ${neighbour.node} as Neighbour from ${curr_node}")
        context.log.info(s"[checkNeighbourstoRemove] Notifying ${neighbour.node} to remove ${curr_node} as neighbour")
        //Notify the node which is not a neighbour to remove current zone node from its neighbours
        neighbour.node ! RemoveNeighbour(currentZone, curr_node)
        Thread.sleep(100)
      }
      else {
        //update the currzone in the neighbour
        context.log.info(s"[checkNeighbourstoRemove] updating ${neighbour.node}'s neighbours with new zone after split of ${curr_node}")
        neighbour.node ! UpdateNeighbour(currentZone, curr_node)
        Thread.sleep(100)
      }
      //Checking which of the current nodes neighbours are neighbours to the new node
      if (isNeighbour(newnodeZone, neighbour.coordinate)) {
        neighbours_to_add_to_newNode.add(neighbour)

      }
    }
    )

    return List(neighbour_remove, neighbours_to_add_to_newNode)
  }

  private def isNeighbour(curr_zone: Coordinate, neighbour_zone: Coordinate): Boolean = {
    if ((isTouchingHorizontally(curr_zone, neighbour_zone) && isSubsetY(curr_zone, neighbour_zone)) || (isTouchingVertically(curr_zone, neighbour_zone) && isSubsetX(curr_zone, neighbour_zone)))
      return true
    else
      return false
  }

  private def isTouchingVertically(curr_zone: Coordinate, neighbour_zone: Coordinate): Boolean = {

    if (neighbour_zone.bottomY == curr_zone.topY || neighbour_zone.topY == curr_zone.bottomY)
      return true
    false
  }

  private def PointInZone(x: Double, y: Double, zone: Coordinate): Boolean = {
    if (x >= zone.leftX && x <= zone.rightX && y >= zone.bottomY && y <= zone.topY)
      true
    else
      false
  }

  private def getEntity(name: String): EntityRef[NodeActor.Command] = {
    sharding.entityRefFor(typekey, name)
  }


}


