package com.kitchenfantasy

import com.kitchenfantasy.datastore.Users
import com.kitchenfantasy.model.{User, UserCredential}
import com.kitchenfantasy.server.api.{Error, Response}

trait KitchenRestAuth {
  def authorizeCredentials (c: UserCredential, f: (User) => Response): Response = {
    Users.read(c.email.toLowerCase) match {
      case Some(u) =>
        if (u.password.equals(c.password) && u.confirmed) f(u)
        else Error(400, "POST credentials are invalid.")
      case None => Error(400, "POST credentials are invalid.")
    }
  }
}
