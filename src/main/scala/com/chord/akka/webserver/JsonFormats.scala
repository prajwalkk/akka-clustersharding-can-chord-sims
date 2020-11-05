package com.chord.akka.webserver
import com.chord.akka.actors.{LookupObject, LookupObjects, NodeGroup}
import spray.json.DefaultJsonProtocol



//#json-formats

object JsonFormats  {
   //import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat2(LookupObject)
  implicit val usersJsonFormat = jsonFormat1(LookupObjects)

  implicit val actionPerformedJsonFormat = jsonFormat1(NodeGroup.ActionSuccessful)
}
//#json-formats
