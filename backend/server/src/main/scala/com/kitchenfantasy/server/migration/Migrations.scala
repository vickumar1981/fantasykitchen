package com.kitchenfantasy.server.migration

import akka.actor.{ActorSystem, Props, actorRef2Scala}
import com.basho.riak.client.IRiakClient
import com.kitchenfantasy.model._
import com.kitchenfantasy.datastore.Products

object MigrationRunner {
  def initialize(client: IRiakClient) = {
    client.createBucket("kitchen-users").nVal(1).r(1).w(1).enableForSearch().lastWriteWins(true).execute()
    client.createBucket("invite-codes").nVal(1).r(1).w(1).enableForSearch().lastWriteWins(true).execute()
    client.createBucket("kitchen-products").nVal(1).r(1).w(1).enableForSearch().lastWriteWins(true).execute()
  }

  def importProducts() = {
    for (product <- ProductInventory.productList) {
      Products.createProduct(product)
      println ("imported product " + product.name + ".\n")
    }
  }
}