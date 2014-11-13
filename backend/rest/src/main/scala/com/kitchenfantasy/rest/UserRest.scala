package com.kitchenfantasy.rest

import com.kitchenfantasy.datastore.Users
import com.kitchenfantasy.model._
import com.kitchenfantasy.server._
import com.kitchenfantasy.server.api._

class UserRest extends Rest {
  def service: PartialFunction[Request, Response] = {

    case POST("user" :: "register" :: Nil, raw) =>
      SerializationProvider.read[User](raw) match {
        case (string, Some(register_user)) =>
          if (Users.hasKey(register_user.email.toLowerCase))
            Error(400, "User '" + register_user.email.toLowerCase + "' already exists.  Unable to register.")
          else {
            println("\nRegistering user '" + register_user.email.toLowerCase + "'\n")
            Users.createUser(register_user)
            JSONResponse(register_user, 1)
          }
        case (string, None) =>
          Error(400, "POST data doesn't conform to type user.")
      }

    case POST("user" :: "login" :: Nil, raw) =>
      SerializationProvider.read[UserCredential](raw) match {
        case (string, Some(user_credential)) =>
          Users.read(user_credential.email.toLowerCase) match {
            case Some(u) =>
              if (LoginValidator.checkIfPWMatch(u.password, user_credential.password))
                JSONResponse (u, 1)
              else Error(400, "POST credentials are invalid.")
            case None => Error(400, "POST credentials are invalid.")
          }
        case (string, None) => Error(400, "POST data doesn't conform to type user credential.")
      }
  }
}

