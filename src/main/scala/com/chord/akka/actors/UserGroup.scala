package com.chord.akka.actors


import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.chord.akka.actors.UserGroup.Command
import com.chord.akka.utils.SystemConstants




object UserGroup {

  var userList = new Array[String](SystemConstants.num_users)
  def apply(): Behavior[Command] =
    Behaviors.setup(context => new UserGroup(context))

  sealed trait Command
  final case class UsersCreated(userList: Array[String]) extends Command


}

class UserGroup(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {

  import UserGroup._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case UsersCreated(path) =>
        userList = path
        this
        }



}