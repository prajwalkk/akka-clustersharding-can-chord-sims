package com.chord.akka.actors
import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.chord.akka.actors.UserGroup.Command
import com.chord.akka.utils.SystemConstants

object UserGroup {

  var UserList = new Array[ActorPath](SystemConstants.num_users)
  def apply(): Behavior[Command] =
    Behaviors.setup(context => new UserGroup(context))

  sealed trait Command
  final case class createUser(num_users: Int) extends Command
  final case class random_actor() extends Command
}

class UserGroup(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {

  import UserGroup._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case createUser(n) =>
        context.log.info(s"Creating $n Users")
        for (i <- 0 until n) {
//          val userId: String =Helper.generateRandomName()
//          val id = Helper.getIdentifier(userId)
          val userId :String =s"User_$i"
          val user = context.spawn(UserActor(userId.toString), userId.toString)
          //context.log.info(s"${Helper.getIdentifier(user.path.toString) % Math.pow(2,SystemConstants.num_users)}")
          UserList(i)= user.path
          context.log.info("User Created " + user.path.toString)

        }

        this


    }
}


