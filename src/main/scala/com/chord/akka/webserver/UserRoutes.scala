package com.chord.akka.webserver

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.chord.akka.actors.UserActor.lookup_data
import com.chord.akka.actors.UserGroup.UserList
import com.chord.akka.actors.{UserActor, lookup_reply}
import com.chord.akka.simulation.Simulation.lookup_data_randomly
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{Await, Future}
import scala.util.Random

class UserRoutes(userRegistry: ActorRef[UserActor.Command])(implicit val system: ActorSystem[_]) extends LazyLogging{

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def lookup(key:String):Future[lookup_reply] = {
    val actor =lookup_data_randomly("Hello")
    logger.info(actor.toString())
    // get random key from data
    actor.ask(lookup_data(key,_))

    //userRegistry.ask(lookup_data("hello",_))
  }


  val userRoutes :Route =
    pathPrefix("lookup"){
    concat(
      pathEnd{
        concat(
          get{
            complete(lookup("hi"))
          },
          post{
            entity(as[lookup_reply]){key =>complete(lookup(key.value)) }
          }
        )
      }
    )}
}





