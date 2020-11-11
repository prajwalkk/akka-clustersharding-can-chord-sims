package com.chord.akka.actors


import akka.actor.TypedActor.dispatcher
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.client.RequestBuilding.Post
import com.chord.akka.actors.UserActor.Command
import com.chord.akka.utils.SystemConstants





object UserActor {

  var userList = new Array[String](SystemConstants.num_users)

  def apply(id: String): Behavior[Command] =
    Behaviors.setup(context => new UserActor(context, id))

sealed trait Command
final case class lookup_data(key:String ) extends Command
final case class put_data(key:String,value:String ) extends Command


}

class UserActor(context: ActorContext[Command], id: String) extends AbstractBehavior[Command](context) {

  import UserActor._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case lookup_data(key) =>
        context.log.info("Key Received "+key)
        //Create a post request here

        this
      case put_data(key,value)=>
        context.log.info("Data Received "+key+" "+value)
      this
    }
}

