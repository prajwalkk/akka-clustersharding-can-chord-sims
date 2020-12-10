package com.can.akka.actors

import org.scalatest.FunSuite
import com.can.akka.utils.Coordinate

class NodeActorTest extends FunSuite{

  test("Points Touching horizontally 1"){
       val testObject = new NodeActor("Node_0",null)
       val curr_zone = Coordinate(0,0,1,2)
       val neighbour_zone = Coordinate(1,0,2,2)
       val expected = true
       val actual =  testObject.isTouchingHorizontally(curr_zone,neighbour_zone)
       assert(actual == expected)
  }

  test("Points Touching horizontally 2"){
    val testObject = new NodeActor("Node_0",null)
    val curr_zone = Coordinate(0,0,1,2)
    val neighbour_zone = Coordinate(2,0,4,2)
    val expected = false
    val actual =  testObject.isTouchingHorizontally(curr_zone,neighbour_zone)
    assert(actual == expected)
  }

  test("Subset Y 1"){
    val testObject = new NodeActor("Node_0",null)
    val curr_zone = Coordinate(0,0,1,2)
    val neighbour_zone = Coordinate(2,0,4,2)
    val expected = true
    val actual =  testObject.isSubsetY(curr_zone,neighbour_zone)
    assert(actual == expected)
  }

  test("Subset Y 2"){
    val testObject = new NodeActor("Node_0",null)
    val curr_zone = Coordinate(0,0,1,2)
    val neighbour_zone = Coordinate(4,4,6,6)
    val expected = false
    val actual =  testObject.isSubsetY(curr_zone,neighbour_zone)
    assert(actual == expected)
  }

  test("Subset X 1"){
    val testObject = new NodeActor("Node_0",null)
    val curr_zone = Coordinate(0,0,1,2)
    val neighbour_zone = Coordinate(0,2,1,4)
    val expected = true
    val actual =  testObject.isSubsetX(curr_zone,neighbour_zone)
    assert(actual == expected)
  }
  test("Subset X 2"){
    val testObject = new NodeActor("Node_0",null)
    val curr_zone = Coordinate(0,0,1,2)
    val neighbour_zone = Coordinate(1,0,2,2)
    val expected = false
    val actual =  testObject.isSubsetX(curr_zone,neighbour_zone)
    assert(actual == expected)
  }

}
