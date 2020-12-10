package com.can.akka.utils

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.EntityRef
import com.can.akka.actors.NodeActor

case class Neighbour(var coordinate: Coordinate,var node: EntityRef[NodeActor.Command]) {

}