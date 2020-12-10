package com.chord.akkasharding.actors


import java.net.URLEncoder

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import com.chord.akkasharding.actors.UserActor.Command
import com.chord.akkasharding.utils.SystemConstants


object UserActor {

  var userList = new Array[String](SystemConstants.num_users)

  def apply(id: String): Behavior[Command] =
    Behaviors.setup(context => new UserActor(context, id).userBehaviors)

  sealed trait Command

  final case class lookup_data(key: String) extends Command

  final case class put_data(key: String, value: String) extends Command

  case class RequestSent() extends Command

}

class UserActor(context: ActorContext[Command], id: String) {

  import UserActor._

  private def userBehaviors: Behavior[UserActor.Command] = {
    Behaviors.receiveMessage {
      case lookup_data(key) =>
        val key1 = URLEncoder.encode(key, "UTF-8")
        // Create a GET request here
        val req = HttpRequest(
          method = HttpMethods.GET,
          uri = s"http://127.0.0.1:8000/chord/$key1"
        )
        Http()(context.system).singleRequest(req)
        Thread.sleep(100)
        Behaviors.same

      case put_data(key, value) =>
        // Create POST request here
        val req = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://127.0.0.1:8000/chord",
          entity = HttpEntity(ContentTypes.`application/json`, s"""{"key":"$key","value":"$value"}""")
        )
        Http()(context.system).singleRequest(req)
        Thread.sleep(100)
        Behaviors.same
    }
  }

}

