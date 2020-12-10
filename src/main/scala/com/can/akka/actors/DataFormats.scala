package com.can.akka.actors

import com.can.akka.utils.Coordinate


final case class LookupObject(value: String)
final case class RequestObject(key:String,value: String)
final case class StoredObject(key:datakey, value:String)

case class datakey(x:Double,y:Double)


