package com.kitchenfantasy.rest

import akka.actor.Props
import com.kitchenfantasy.KitchenRestAuth
import com.kitchenfantasy.datastore.{Products, Orders}
import com.kitchenfantasy.jobs._
import com.kitchenfantasy.model._
import com.kitchenfantasy.server.SerializationProvider
import com.kitchenfantasy.server.api._

class ProductsRest extends Rest with KitchenRestAuth {
  def service: PartialFunction[Request, Response] = {
    case GET("products" :: Nil) => {
      val products = Products.findProducts
      JSONResponse(products.sortBy(p => (p.name, p.id)), products.size)
    }

    case POST("products" :: "orders" :: "q" :: Nil, raw) =>
      SerializationProvider.read[OrderSearch](raw) match {
        case (string, Some(search)) =>
          authorizeCredentials(search.credential, (u) => {
            if (u.is_admin) {
              val orders = Orders.findByQuery(search.query)
                .filter (o => search.query.status.isEmpty ||
                  search.query.status.equalsIgnoreCase(o.status))
                .sortBy(o => {
                if (o.timestamp.isDefined)
                  -o.timestamp.get
                else
                  -System.currentTimeMillis
              })
              JSONResponse(orders, orders.size)
            }
            else Error(400, "POST credentials are invalid.")
          })
        case (string, None) => Error(400, "POST data doesn't conform to type user credential.")
      }

    case POST("products" :: "orders" :: Nil, raw) =>
      SerializationProvider.read[UserCredential](raw) match {
        case (string, Some(credential)) =>
          authorizeCredentials(credential, (u) => {
            val orders = Orders.findByEmail(u.email).sortBy(o => {
              if (o.timestamp.isDefined)
                - o.timestamp.get
              else
                - System.currentTimeMillis
            }).slice(0, 30)
            JSONResponse(orders, orders.size)
          })
        case (string, None) => Error(400, "POST data doesn't conform to type user credential.")
      }

    case POST("products" :: "order" :: "status" :: Nil, raw) =>
      SerializationProvider.read[OrderUpdate](raw) match {
        case (string, Some(update)) =>
          authorizeCredentials(update.credential, (u) => {
            if (u.is_admin)
              Orders.read(update.order_id) match {
                case Some(o) => {
                  if (!o.status.equalsIgnoreCase("refund") &&
                    update.status.equalsIgnoreCase("refund")) {
                    if (CCPaymentJob.refundPayment(o)) {
                      val newOrder = Orders.updateStatus(o, update.status.toLowerCase)
                      val emailSender = JobSettings.processor.actorOf(Props[SendEmailJob],
                        "refund_order_%s".format(o.id.getOrElse(System.currentTimeMillis.toString)))
                      emailSender ! OrderRefundEmail (newOrder)
                      JSONResponse(newOrder, 1)
                    }
                    else Error(400, "Unable to refund order '%s'.".format(update.order_id.toLowerCase))
                  }
                  else if (o.status.equalsIgnoreCase("approved") &&
                    update.status.equalsIgnoreCase("completed")) {
                    val newOrder = Orders.updateStatus(o, update.status.toLowerCase)
                    val emailSender = JobSettings.processor.actorOf(Props[SendEmailJob],
                      "complete_order_%s".format(o.id.getOrElse(System.currentTimeMillis.toString)))
                    emailSender ! OrderCompletionEmail (newOrder)
                    JSONResponse(newOrder, 1)
                  }
                  else Error(400, "Status is invalid.")
                }
                case None => Error(400, "No order found with id '%s'.".format(update.order_id))
              }
            else Error(400, "POST credentials are invalid.")
          })
        case (string, None) => Error(400, "POST data doesn't conform to type user credential.")
      }

    case POST("products" :: "order" :: "email" :: Nil, raw) =>
      SerializationProvider.read[OrderContactInfo](raw) match {
        case (string, Some(orderContact)) =>
          Orders.read(orderContact.order_id) match {
            case Some(o) => {
              val emailSender = JobSettings.processor.actorOf(Props[SendEmailJob],
                "user_order_info_%s_%s".format(o.id.getOrElse(""), System.currentTimeMillis))
              emailSender ! OrderInfoEmail(o.id.get, o.email, orderContact.info)
              JSONResponse("OK", 1)
            }
            case _ => Error(400, "No order found with id '%s'.".format(orderContact.order_id))
          }
        case (string, None) => Error(400, "POST data doesn't conform to type order contact information.")
      }

    case POST("products" :: "order" :: Nil, raw) =>
      SerializationProvider.read[Transaction](raw) match {
        case (string, Some(transaction)) =>
          authorizeCredentials(transaction.credential, (u) => {
            if (u.credit_cards.isDefined && (u.credit_cards.get.size > 0)) {
              if (u.address.isDefined) {
                val products: List[Product] = transaction.order.filter(_.qty.getOrElse(0) > 0).map {
                  p => Products.read(p.id) match {
                    case Some(foundProduct) => Some(foundProduct.copy(qty=Some(p.qty.getOrElse(0))))
                    case _ => None
                  }
                }.filter(_.isDefined).map{ p => p.get }.toList

                if (products.size > 0) {
                  val (total, subtotal, tax) = OrderValidator.orderTotals(products)
                  val o = Order(u.email, u.credit_cards.get(0), u.address.get, products,
                    Some(total), Some(subtotal), Some(tax))

                  CCPaymentJob.processPayment(o) match {
                    case Some(result) => {
                      val newOrder = Orders.createOrder(o, result._1, result._2)
                      val emailSender = JobSettings.processor.actorOf(Props[SendEmailJob],
                        "confirm_order_%s".format(newOrder.id.getOrElse(System.currentTimeMillis.toString)))
                      emailSender ! OrderConfirmationEmail(newOrder)
                      JSONResponse(newOrder, 1)
                    }
                    case _ => Error(400, "Credit card information is invalid.")
                  }
                }
                else Error(400, "There are no products listed in the order.")
              }
              else Error(400, "User has no shipping address listed.")
            }
            else Error(400, "User has no credit cards listed.")
          })
        case (string, None) => Error(400, "POST data doesn't conform to type order update.")
      }
  }
}
