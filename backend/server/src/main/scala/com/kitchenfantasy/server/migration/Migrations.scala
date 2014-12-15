package com.kitchenfantasy.server.migration

import com.basho.riak.client.IRiakClient
import com.kitchenfantasy.model._
import com.kitchenfantasy.datastore.{Products, Users}

object MigrationRunner {
  def initialize(client: IRiakClient) = {
    client.createBucket("kitchen-users").nVal(1).r(1).w(1).enableForSearch().lastWriteWins(true).execute()
    client.createBucket("kitchen-invite-codes").nVal(1).r(1).w(1).enableForSearch().lastWriteWins(true).execute()
    client.createBucket("kitchen-products").nVal(1).r(1).w(1).enableForSearch().lastWriteWins(true).execute()
    client.createBucket("kitchen-orders").nVal(1).r(1).w(1).enableForSearch().lastWriteWins(true).execute()
  }

  def importProducts() =
    for (product <- ProductInventory.productList) {
      Products.createProduct(product)
      println ("imported product " + product.name + ".")
    }

  def importAdmins() =
    for (admin <- AdminUsers.adminUserList)
      Users.read(admin.toLowerCase) match {
        case Some(u) => {
          Users.makeAdmin(u)
          println ("user '" + u.email + "' made administrator.")
        }
        case _ => println ("user '" + admin+ "' not found.")
      }
}