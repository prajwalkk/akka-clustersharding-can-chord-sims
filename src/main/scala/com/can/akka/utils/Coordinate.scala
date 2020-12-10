package com.can.akka.utils

case class Coordinate(var leftX: Double, var bottomY:Double, var rightX:Double, var topY:Double)
{
  override def toString: String = s"Bottom Left ($leftX,$bottomY) Top Right ($rightX,$topY)"
}


