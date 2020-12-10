package com.chord.akkasharding.actors

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.chord.akkasharding.actors.UserGroup.Command
import com.chord.akkasharding.utils.SystemConstants

object UserGroup {

  var UserList = new Array[ActorPath](SystemConstants.num_users)

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new UserGroup(context).userGroupBehaviors)

  sealed trait Command

  final case class createUser(num_users: Int) extends Command

}

class UserGroup(context: ActorContext[Command]) {

  import UserGroup._

  private def userGroupBehaviors: Behavior[Command] = {
    Behaviors.receiveMessage {

      case createUser(n) =>
        context.log.info(s"Creating $n Users")
        for (i <- 0 until n) {
          val userId: String = s"User_$i"
          val user = context.spawn(UserActor(userId), userId)
          UserList(i) = user.path
          context.log.info("User Created " + user.path.toString)
        }
        Behaviors.same
    }
  }
}


