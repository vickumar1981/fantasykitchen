package com.kitchenfantasy.jobs

import akka.actor.ActorSystem
import com.kitchenfantasy.server.GlobalConfiguration
import org.slf4j.LoggerFactory

object JobSettings {
  lazy val logger = LoggerFactory.getLogger("com.kitchenfantasy.server")
  lazy val processor = ActorSystem("KitchenJobsProcessor")
  lazy val config = GlobalConfiguration.config

  object email {
    lazy val username: String = config.getString("email.user")
    lazy val password: String = config.getString("email.password")
    lazy val from: String = config.getString("email.from")
    lazy val auth: String = config.getString("email.smtp.auth")
    lazy val startTLSEnabled: String = config.getString("email.smtp.startTLS")
    lazy val host = config.getString("email.smtp.host")
    lazy val port = config.getInt("email.smtp.port")
  }

  object payment {
    lazy val paypalMode: String = config.getString("paypal.mode")
    lazy val paypalClientId: String = config.getString("paypal.client.id")
    lazy val paypalClientSecret: String = config.getString("paypal.client.secret")
  }
}

object EmailTemplates {
  object order_info {
    def subject (order_id: String, from: String) = "%s: Order - %s".format(from, order_id)
    def body (order_id: String, from: String, body: String) =
      """
        |User: %s
        |
        |Order Id: %s
        |
        |Comment: %s
        |
      """.format(from, order_id, body).stripMargin
  }

  object complete_order {
    lazy val subject = "Thanks! Your Order is on its way!"

    def body(order_id: String, cc_number: String, total: String) =
      """
        |Thank you!  Your order is on its way.
        |Please expect delivery in 2-3 business days.
        |
        |
        |Review the details of your order below:
        |
        |
        |    Order Id: %s
        |    CC #: %s
        |    Total: %s
        |
        |
        |
        |***** DO NOT RESPOND TO THIS MESSAGE *****
        |
      """.format(order_id, cc_number, total).stripMargin
  }

  object refund_order {
    lazy val subject = "Your Order has been refunded."

    def body(order_id: String, cc_number: String, total: String) =
      """
        |Thank you!  Your order has been successfully refunded.
        |Please allow 5-6 business days for the refunded funds to
        |be processed into your account.
        |
        |
        |Review the details of your order below:
        |
        |
        |    Order Id: %s
        |    CC #: %s
        |    Total: %s
        |
        |
        |
        |***** DO NOT RESPOND TO THIS MESSAGE *****
        |
      """.format(order_id, cc_number, total).stripMargin
  }

  object confirm_order {
    lazy val subject = "Thanks! Your Order has been confirmed."

    def body(order_id: String, cc_number: String, total: String) =
      """
        |Thank you!  Your order has been confirmed.
        |
        |Please review the information about your order below:
        |
        |
        |    Order Id: %s
        |    CC #: %s
        |    Total: %s
        |
        |
        |
        |***** DO NOT RESPOND TO THIS MESSAGE *****
        |
      """.format(order_id, cc_number, total).stripMargin
  }

  object registration {
    lazy val subject = "Welcome to Fantasy Kitchen"
    def body (code: String) =
      """
        |Welcome to Fantasy Kitchen.
        |
        |Thank you for registering!
        |
        |Please use the following verification code
        |to complete your registration
        |
        |
        |Verification Code: %s
        |
        |
        |
        |***** DO NOT RESPOND TO THIS MESSAGE *****
        |
      """.format(code).stripMargin
  }

  object pw_reminder {
    lazy val subject = "Update your Fantasy Kitchen password"
    def body (code: String) =
      """
        |Update your Fantasy Kitchen password.
        |
        |If you did not request a password reminder please disregard this email
        |
        |Please use the following verification code
        |to update your password
        |
        |
        |Verification Code: %s
        |
        |
        |
        |***** DO NOT RESPOND TO THIS MESSAGE *****
        |
      """.format(code).stripMargin
  }
}