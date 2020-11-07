package com.chord.akka.webserver
import com.chord.akka.actors.{LookupObject, LookupObjects, NodeGroup, lookup_reply}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}



//#json-formats

object JsonFormats  {
  //import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[LookupObject] = jsonFormat2(LookupObject)
  implicit val usersJsonFormat: RootJsonFormat[LookupObjects] = jsonFormat1(LookupObjects)

  implicit val actionPerformedJsonFormat: RootJsonFormat[NodeGroup.ActionSuccessful] = jsonFormat1(NodeGroup.ActionSuccessful)
  implicit  val lookup_replyJson = jsonFormat1(lookup_reply)

}
//#json-formats