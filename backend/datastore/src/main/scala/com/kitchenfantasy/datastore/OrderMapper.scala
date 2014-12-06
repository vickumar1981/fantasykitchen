package com.kitchenfantasy.datastore

import com.kitchenfantasy.datastore.base.RiakMapper
import com.kitchenfantasy.model.Order

object Orders extends RiakMapper[Order]("kitchen-orders") {
  private def addIndexes (o: Order): Order = {
    if (o.id.isDefined) {
      addIndex(o.id.get, "email", o.email.toLowerCase)
      if (o.timestamp.isDefined)
        addIndex(o.id.get, "timestamp", o.timestamp.get)
      if (o.promo.isDefined)
        addIndex(o.id.get, "promo", o.promo.get.id)
    }
    o
  }

  def createOrder (o: Order): Order = {
    val timeStamp = System.currentTimeMillis
    val newId = timeStamp + "_" + generateId.substring(0, 10)
    val newOrder = o.copy (id=Some(newId), timestamp=Some(timeStamp))
    create (newId, newOrder)
    addIndexes (newOrder)
  }
}