package com.chord.akka.actors


import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, MessageEntity}
import com.chord.akka.actors.UserActor.Command
import com.chord.akka.utils.SystemConstants





object UserActor {

  var userList = new Array[String](SystemConstants.num_users)
  def apply(id: String): Behavior[Command] =
    Behaviors.setup(context => new UserActor(context, id))

sealed trait Command
final case class lookup_data(key:String) extends Command
final case class put_data(key:String,value:String ) extends Command


}

class UserActor(context: ActorContext[Command], id: String) extends AbstractBehavior[Command](context) {

  import UserActor._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case lookup_data(key) =>
        context.log.info("Key "+key)
        //Create a post request here
        val req =HttpRequest(
          method = HttpMethods.GET,
          uri = s"http://127.0.0.1:8080/chord/$key"

        )
        Http()(context.system).singleRequest(req)
        this
      case put_data(key,value)=>


        val req =HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://127.0.0.1:8080/chord",
          entity = HttpEntity(ContentTypes.`application/json` ,s"""{"key":"$key","value":"$value"}""")
        )
        Http()(context.system).singleRequest(req)

      this
    }
}

