package com.kitchenfantasy.datastore

import com.basho.riak.client.query.functions.JSSourceFunction
import com.kitchenfantasy.datastore.base.RiakMapper
import com.kitchenfantasy.model.{Order, OrderQuery}

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


  def searchOrders (text: String) = new JSSourceFunction(
    """
      |function(value, keyData, arg) {
      | var data = Riak.mapValuesJson(value)[0];
      | var search = data.email + ',' + data.credit_card.first_name + ' ' +
      |   data.credit_card.last_name + ',' + data.id + ',' + data.transaction_id;
      | if (search.toLowerCase().indexOf('%s') > -1)
      |   return [data];
      | return [];
      |}
    """.format(text.toLowerCase).stripMargin)

  def findByEmail (email: String): List[Order] = findByIndex("email", email)

  def findByQuery (query: OrderQuery) =
    if (query.text.length > 0)
      findByIndex("timestamp", query.start_date, query.end_date + 86400000, Some(searchOrders(query.text)))
    else
      findByIndex("timestamp", query.start_date, query.end_date + 86400000)

  def createOrder (o: Order, transaction_id: String): Order = {
    val timeStamp = System.currentTimeMillis
    val newId = timeStamp + "_" + generateId.substring(0, 10)
    val newOrder = o.copy (id=Some(newId), timestamp=Some(timeStamp), transaction_id=Some(transaction_id))
    create (newId, newOrder)
    addIndexes (newOrder)
  }
}