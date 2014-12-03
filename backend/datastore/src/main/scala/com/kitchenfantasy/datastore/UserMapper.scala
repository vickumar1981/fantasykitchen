package com.kitchenfantasy.datastore

import com.kitchenfantasy.datastore.base.RiakMapper
import com.kitchenfantasy.model.User

object Users extends RiakMapper[User]("kitchen-users") {
  private def addIndexes (u: User) = {
    addIndex (u.email.toLowerCase, "email", u.email.toLowerCase)
    addIndex (u.email.toLowerCase, "confirmed", u.confirmed.toString.toLowerCase)
    addIndex (u.email.toLowerCase, "is_admin", u.is_admin.toString.toLowerCase)
    u.email.toLowerCase
  }

  def createUser (u: User) {
    val id = create (u.email.toLowerCase, u)
    addIndexes (u)
  }

  def updateUser (u: User) {
    update (u.email.toLowerCase, u)
    addIndexes (u)
  }
}