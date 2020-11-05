package Server

import Utils.Helper

import scala.collection.mutable.ListBuffer

object InitServer {

  val bitSize = 24
  val server:Server = new Server("FirstServer"+Helper.generateRandomName())

  var fingerTableBuffer:ListBuffer[FingerTableEntity] = new ListBuffer()
  for(i <- 1 to bitSize-1) {
    val start: Int = server.identity.identifier + Math.pow(2, i - 1).asInstanceOf[Int]
    val end: Int = server.identity.identifier + Math.pow(2, i).asInstanceOf[Int]
    val interval: Array[Int] = Array(start, end)
    val fingerEntity:FingerTableEntity = new FingerTableEntity(interval,server)
    fingerTableBuffer += fingerEntity
  }

  server.fingerTable = fingerTableBuffer.toList

  def get():Server=server
}
