package com.kitchenfantasy.jobs

import akka.actor.ActorSystem
import com.kitchenfantasy.server.GlobalConfiguration

import org.slf4j.Logger
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
  object confirm_order {
    lazy val subject = "Thanks! Your Order has been confirmed."

    def body (order_id: String, cc_number: String, total: String) =
    { "Thank you!  Your order has been confirmed.\n\n" +
      "Please review the information about your order below: \n\n\n" +
      "\tOrder Id: " + order_id + "\n" +
      "\tCC #: " + cc_number + "\n" +
      "\tTotal: " + total + "\n\n\n\n" +
      "***** DO NOT RESPOND TO THIS MESSAGE *****\n\n" }
  }

  object registration {
    lazy val subject = "Welcome to Fantasy Kitchen"

    def body (code: String) = { "Welcome to Fantasy Kitchen.\n\n" +
      "Thank you for registering!\n\n" +
      "Please use the following verification code\n" +
      "to complete your registration\n\n\n" +
      "Verification Code: " + code + "\n\n\n\n" +
      "***** DO NOT RESPOND TO THIS MESSAGE *****\n\n" }

    /*
    def body {code: String} = {
      <html lang="en">
        <head></head>
        <body>
        </body>
      </html>
    }
    */
  }
}