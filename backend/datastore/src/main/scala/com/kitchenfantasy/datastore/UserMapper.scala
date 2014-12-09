package com.kitchenfantasy.datastore

import com.kitchenfantasy.datastore.base.RiakMapper
import com.kitchenfantasy.model.{InviteCode, UserUpdate, User}

object Users extends RiakMapper[User]("kitchen-users") {
  private def addIndexes (u: User): User = {
    addIndex (u.email.toLowerCase, "email", u.email.toLowerCase)
    addIndex (u.email.toLowerCase, "confirmed", u.confirmed.toString.toLowerCase)
    addIndex (u.email.toLowerCase, "is_admin", u.is_admin.toString.toLowerCase)
    u
  }

  def updatePw (u: User, newPassword: String): User = {
    val newUser = u.copy(password = newPassword)
    update (u.email.toLowerCase, newUser)
    addIndexes (newUser)
  }

  def updateInvite (u: User): User = {
    val newUser = u.copy(invite_code = Some(generateId.substring(0, 10)))
    update (u.email.toLowerCase, newUser)
    addIndexes (newUser)
  }

  def createUser (invite: InviteCode): User = {
    val newUser = invite.user.copy(confirmed = true, invite_code = Some(invite.code))
    val id = create (newUser.email.toLowerCase, newUser)
    addIndexes (newUser)
  }

  def updateUser (u: User, info: UserUpdate): User = {
    val newUser = u.copy(address=Some(info.address), credit_cards = Some(List(info.credit_card)))
    update (u.email.toLowerCase, newUser)
    addIndexes (newUser)
  }
}