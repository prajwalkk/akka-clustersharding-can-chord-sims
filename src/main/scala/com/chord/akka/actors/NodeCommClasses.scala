package com.chord.akka.actors

/*
*
* Created by: prajw
* Date: 05-Nov-20
*
*/
final case class LookupObject(value: String)
final case class RequestObject(key:String,value: String)
final case class StoredObject(key:Int,value:String)

final case class LookupObjects(objs: Seq[String])

// Gets the server's Identity
case class Identity(name: String, identifier: Int)


