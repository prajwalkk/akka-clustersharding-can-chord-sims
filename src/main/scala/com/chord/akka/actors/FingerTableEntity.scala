package com.chord.akka.actors

/*
*
* Created by: prajw
* Date: 10-Nov-20
*
*/
case class FingerTableEntity(start: Int,
                             startInterval: Int,
                             endInterval: Int,
                             node: Int,
                             successor: Int,
                             predecessor: Int) {

  def getInterval: Range = (startInterval until endInterval)
}
