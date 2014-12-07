package com.kitchenfantasy.datastore

import com.kitchenfantasy.datastore.base.RiakMapper
import com.kitchenfantasy.model.Order

object Orders extends RiakMapper[Order]("kitchen-orders") {
  private def addIndexes (o: Order): Order = {
    if (o.id.isDefined) {
      addIndex(o.id.get, "email", o.email.toLowerCase)
      if (o.timestamp.isDefined)
        addIndex(o.id.get, "timestamp", o.timestamp.get)
      if (o.transaction_id.isDefined)
        addIndex(o.id.get, "transaction_id", o.transaction_id.get)
      if (o.promo.isDefined)
        addIndex(o.id.get, "promo", o.promo.get.id)
    }
    o
  }

  def findByEmail (email: String): List[Order] = findByIndex("email", email)

  def createOrder (o: Order, transaction_id: String): Order = {
    val timeStamp = System.currentTimeMillis
    val newId = timeStamp + "_" + generateId.substring(0, 10)
    val newOrder = o.copy (id=Some(newId), timestamp=Some(timeStamp), transaction_id=Some(transaction_id))
    create (newId, newOrder)
    addIndexes (newOrder)
  }
}