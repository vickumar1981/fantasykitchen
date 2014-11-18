package com.kitchenfantasy.jobs

import java.util.Properties
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

import akka.actor.Actor
import akka.actor.ActorSystem
import com.kitchenfantasy.server.GlobalConfiguration

import com.typesafe.config.{ConfigFactory, Config}

import com.kitchenfantasy.model.InviteCode

case class SendRegistrationEmail (invite: InviteCode)

object RegistrationEmail {
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

object EmailSettings {
  lazy val processor = ActorSystem("EmailProcessor")

  lazy val config = GlobalConfiguration.config

  lazy val username: String = config.getString("email.user")
  lazy val password: String = config.getString("email.password")
  lazy val from: String = config.getString("email.from")
  lazy val auth: String = config.getString("email.smtp.auth")
  lazy val startTLSEnabled: String = config.getString("email.smtp.startTLS")
  lazy val host = config.getString("email.smtp.host")
  lazy val port = config.getInt("email.smtp.port")
}

class SendEmailJob extends Actor {

  private def sendRegistrationEmail(job: SendRegistrationEmail) = {
    val props = new Properties()
    props.put("mail.smtp.auth", EmailSettings.auth)
    props.put("mail.smtp.starttls.enable", EmailSettings.startTLSEnabled)
    props.put("mail.smtp.host", EmailSettings.host)
    props.put("mail.smtp.port", EmailSettings.port.toString)

    val session = Session.getInstance(props,
      new javax.mail.Authenticator() {
        protected override def getPasswordAuthentication(): PasswordAuthentication = {
          new PasswordAuthentication(EmailSettings.username, EmailSettings.password)
        }
      })

    try {
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(EmailSettings.from))
      message.setRecipients(Message.RecipientType.TO, job.invite.user.email.toLowerCase)
      message.setSubject(RegistrationEmail.subject)
      message.setText(RegistrationEmail.body(job.invite.code))

      Transport.send(message)
      println("\nSent registration email to '" + job.invite.user.email.toLowerCase + "'\n")

    } catch {
      case (e: MessagingException) => println("\nError sending registration email to  '"
        + job.invite.user.email.toLowerCase + "'\n")
    }
  }

  def receive = {
    case (job: SendRegistrationEmail) => {
      sendRegistrationEmail(job)
      context.stop(self)
    }
  }
}
