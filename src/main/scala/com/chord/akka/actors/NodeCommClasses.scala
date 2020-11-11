package com.chord.akka.actors

/*
*
* Created by: prajw
* Date: 05-Nov-20
*
*/
final case class LookupObject(key: String, value: String)
final case class StoredObject(key:Int,value:String)

final case class LookupObjects(objs: Seq[StoredObject])

// Gets the server's Identity
case class Identity(name: String, identifier: Int)


