package com.kitchenfantasy.datastore

import com.kitchenfantasy.datastore.base.RiakMapper
import com.kitchenfantasy.model.InviteCode
import com.kitchenfantasy.model.User

object InviteCodes extends RiakMapper[InviteCode]("kitchen-invite-codes") {
  private def addIndexes (ic: InviteCode) = {
    addIndex (ic.user.email.toLowerCase, "email", ic.user.email.toLowerCase)
    ic.user.email.toLowerCase
  }

  def createInviteCode (u: User): InviteCode = {
    val ic = InviteCode (u, generateId.substring(0, 10))
    create (u.email.toLowerCase, ic)
    addIndexes (ic)
    ic
  }
}

