package com.can.akka.actors

import java.net.URLEncoder

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import com.can.akka.actors.User.Command
import com.can.akka.utils.Helper

import scala.collection.mutable.ArrayBuffer

object User {
  var userList = new ArrayBuffer[ActorPath](50)

  def apply(id: String): Behavior[Command] =
    Behaviors.setup(context => new User(context, id).userBehaviors)

  sealed trait Command

  final case class lookup_data(key: String) extends Command

  final case class put_data(key: String, value: String) extends Command

  final case class createUser() extends Command

  case class RequestSent() extends Command

}

class User(context: ActorContext[Command], id: String) {

  import User._

  private def userBehaviors: Behavior[User.Command] = {
    Behaviors.receiveMessage {
      case createUser() => {
        val userId = s"User_${Helper.generateRandomName()}"
        val user = context.spawn(User(userId), userId)
        userList.append(user.path)
        context.log.info("User Created " + user.path.toString)
        Behaviors.same
      }

      case lookup_data(key) =>
        val key1 = URLEncoder.encode(key, "UTF-8")
        // Create a GET request here
        val req = HttpRequest(
          method = HttpMethods.GET,
          uri = s"http://127.0.0.1:8000/can/$key1"
        )
        Http()(context.system).singleRequest(req)
        Thread.sleep(100)
        Behaviors.same

      case put_data(key, value) =>
        // Create POST request here
        val req = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://127.0.0.1:8000/can",
          entity = HttpEntity(ContentTypes.`application/json`, s"""{"key":"$key","value":"$value"}""")
        )
        Http()(context.system).singleRequest(req)
        Thread.sleep(100)
        Behaviors.same
    }
  }

}
