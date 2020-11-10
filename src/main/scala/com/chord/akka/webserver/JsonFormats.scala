package com.chord.akka.webserver
import com.chord.akka.actors.{LookupObject, LookupObjects, NodeActor}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}



//#json-formats

object JsonFormats  {
  //import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[LookupObject] = jsonFormat2(LookupObject)
  implicit val usersJsonFormat: RootJsonFormat[LookupObjects] = jsonFormat1(LookupObjects)

  implicit val actionPerformedJsonFormat: RootJsonFormat[NodeActor.ActionSuccessful] = jsonFormat1(NodeActor.ActionSuccessful)
}
//#json-formats