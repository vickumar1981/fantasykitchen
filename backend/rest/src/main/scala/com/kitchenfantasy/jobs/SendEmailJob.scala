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
import com.kitchenfantasy.model.{InviteCode, Order, OrderValidator}

case class RegistrationEmail (invite: InviteCode)
case class OrderConfirmationEmail (order: Order)

class SendEmailJob extends Actor {

  private def emailProperties = {
    val props = new Properties()
    props.put("mail.smtp.auth", JobSettings.email.auth)
    props.put("mail.smtp.starttls.enable", JobSettings.email.startTLSEnabled)
    props.put("mail.smtp.host", JobSettings.email.host)
    props.put("mail.smtp.port", JobSettings.email.port.toString)
    props
  }

  private def emailSession (props: Properties) = {
    val session = Session.getInstance(props,
      new javax.mail.Authenticator() {
        protected override def getPasswordAuthentication(): PasswordAuthentication = {
          new PasswordAuthentication(JobSettings.email.username, JobSettings.email.password)
        }
      })
    session
  }

  private def emailMessage (s: Session, to:String, subject: String, text:String) = {
    val message = new MimeMessage(s)
    message.setFrom(new InternetAddress(JobSettings.email.from))
    message.setRecipients(Message.RecipientType.TO, to.toLowerCase)
    message.setSubject(subject)
    message.setText(text)
    message
  }

  private def sendOrderConfirmationEmail(job: OrderConfirmationEmail) = {
    val props = emailProperties
    val session = emailSession (props)

    try {
      val message = emailMessage(session, job.order.email, EmailTemplates.confirm_order.subject,
                                  EmailTemplates.confirm_order.body(job.order.id.getOrElse(""),
                                  "xxxx" + (job.order.credit_card.cc_number takeRight 4),
                                  OrderValidator.formatPrice(job.order.total.getOrElse(0L))))
      Transport.send(message)
      println("\nSent order confirmation email to '" + job.order.email.toLowerCase + "'\n")
    } catch {
      case (e: MessagingException) => println("\nError sending order confirmation email to  '"
        + job.order.email.toLowerCase + "'\n")
    }
  }

  private def sendRegistrationEmail(job: RegistrationEmail) = {
    val props = emailProperties
    val session = emailSession (props)

    try {
      val message = emailMessage(session, job.invite.user.email, EmailTemplates.registration.subject,
                      EmailTemplates.registration.body(job.invite.code))
      Transport.send(message)
      println("\nSent registration email to '" + job.invite.user.email.toLowerCase + "'\n")

    } catch {
      case (e: MessagingException) => println("\nError sending registration email to  '"
        + job.invite.user.email.toLowerCase + "'\n")
    }
  }

  def receive = {
    case (job: RegistrationEmail) => {
      sendRegistrationEmail(job)
      context.stop(self)
    }

    case (job: OrderConfirmationEmail) => {
      sendOrderConfirmationEmail(job)
      context.stop(self)
    }
  }
}
