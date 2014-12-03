package com.kitchenfantasy.rest

import com.kitchenfantasy.KitchenRestAuth
import com.kitchenfantasy.datastore.{Users, InviteCodes}
import com.kitchenfantasy.jobs._
import com.kitchenfantasy.model._
import com.kitchenfantasy.server._
import com.kitchenfantasy.server.api._
import akka.actor.Props
import akka.actor.actorRef2Scala

class UserRest extends Rest with KitchenRestAuth {
  def service: PartialFunction[Request, Response] = {
    case POST("user" :: "info" :: Nil, raw) =>
      SerializationProvider.read[UserUpdate](raw) match {
        case (string, Some(update)) =>
          authorizeCredentials(update.credential, (u) => {
            val newUser = u.copy(address=Some(update.address), credit_cards = Some(List(update.credit_card)))
            Users.updateUser(newUser)
            JSONResponse(newUser, 1)
          })
        case (string, None) => Error(400, "POST data doesn't conform to type user update.")
      }

    case POST("user" :: "register" :: Nil, raw) =>
      SerializationProvider.read[User](raw) match {
        case (string, Some(register_user)) => {
          val email = register_user.email.toLowerCase
          if (Users.hasKey(email))
            Error(400, "User '" + email + "' already exists.  Unable to register.")
          else
            register_user.invite_code match {
              case None => {
                println("\nRegistering user '" + email + "'\n")
                val invite = InviteCodes.createInviteCode(register_user.copy(email=email))
                val emailSender = EmailSettings.processor.actorOf(Props[SendEmailJob],
                  "register_user" + "_" + System.currentTimeMillis.toString)
                emailSender ! SendRegistrationEmail(invite)
                JSONResponse(register_user.copy(confirmed = false), 1)
              }
              case Some(code) =>
                InviteCodes.read(email) match {
                  case None => Error(404, "No invite code found for user '" + email + "'.")
                  case Some(invite) => {
                    if (code.equals(invite.code)) {
                      println("\nVerifying user '" + email + "'\n")
                      val newUser = invite.user.copy(confirmed = true, invite_code = Some(invite.code))
                      Users.createUser(newUser)
                      InviteCodes.delete(email)
                      JSONResponse(newUser, 1)
                    }
                    else Error (404, "The invite code is invalid.")
                  }
                }
            }
        }
        case (string, None) => Error(400, "POST data doesn't conform to type user.")
      }

    case POST("user" :: "login" :: Nil, raw) =>
      SerializationProvider.read[UserCredential](raw) match {
        case (string, Some(user_credential)) =>
          Users.read(user_credential.email.toLowerCase) match {
            case Some(u) =>
              if ((LoginValidator.checkIfPWMatch(u.password, user_credential.password)) && (u.confirmed)) {
                println("\nLogging in user '" + u.email + "'\n")
                JSONResponse(u.copy(invite_code = None), 1)
              }
              else Error(400, "POST credentials are invalid.")
            case None => Error(400, "POST credentials are invalid.")
          }
        case (string, None) => Error(400, "POST data doesn't conform to type user credential.")
      }
  }
}

