package com.chord.akka.simulation
import com.chord.akka.utils.SystemConstants
import com.chord.akka.actors.{NodeGroup, UserGroup}
import org.scalatest.FunSuite

class SimulationTest extends FunSuite{
  val simulationObject =  Simulation

  test("Node Length Check"){
    val nodeLength = NodeGroup.NodeList.length
    val expectedLength = SystemConstants.num_nodes
    assert(nodeLength == expectedLength)
  }

  test("User Length Check"){
    val userLength = UserGroup.UserList.length
    val expectedLength = SystemConstants.num_users
    assert(userLength == expectedLength)
  }

  simulationObject.nodeActorSystem.terminate()
  simulationObject.userActorSystem.terminate()

}
