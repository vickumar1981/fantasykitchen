package com.kitchenfantasy.jobs

import akka.actor.ActorSystem
import com.kitchenfantasy.server.GlobalConfiguration

object JobSettings {
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
    lazy val paypalClientId: String = config.getString("paypal.client_id")
    lazy val paypalClientSecret: String = config.getString("email.client_secret")
  }
}
