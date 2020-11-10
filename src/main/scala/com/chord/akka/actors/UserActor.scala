package com.chord.akka.actors


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import com.chord.akka.actors.UserActor.Command
import com.chord.akka.utils.SystemConstants





object UserActor {

  var userList = new Array[String](SystemConstants.num_users)

  def apply(id: String): Behavior[Command] =
    Behaviors.setup(context => new UserActor(context, id))

sealed trait Command
final case class lookup_data(key:String,replyTo:ActorRef[LookupObject] ) extends Command
final case class put_data(key:String,value:String ) extends Command


}

class UserActor(context: ActorContext[Command], id: String) extends AbstractBehavior[Command](context) {

  import UserActor._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case lookup_data(key,replyTo) =>
        context.log.info("Key Received "+key)
//        replyTo ! LookupObject(key)
       // val response = Http()(context.system).singleRequest(HttpRequest(uri="http://localhost:8080/chord").addAttribute("key",key))

        this
      case put_data(key,value)=>
        context.log.info("Data Received "+key+" "+value)
      this
    }
}

