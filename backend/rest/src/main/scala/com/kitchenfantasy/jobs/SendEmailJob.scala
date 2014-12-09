package com.kitchenfantasy.jobs

import java.util.Properties
import javax.mail.{Message, MessagingException, PasswordAuthentication, Session, Transport}
import javax.mail.internet.{InternetAddress, MimeMessage}

import akka.actor.Actor
import com.kitchenfantasy.model.{UserCredential, InviteCode, Order, OrderValidator}

case class RegistrationEmail (invite: InviteCode)
case class ForgotPwEmail (credential: UserCredential)
case class OrderConfirmationEmail (order: Order)
case class OrderInfoEmail (order_id: String, from: String, info: String)

class SendEmailJob extends Actor {
  private def emailProperties = {
    val props = new Properties()
    props.put("mail.smtp.auth", JobSettings.email.auth)
    props.put("mail.smtp.starttls.enable", JobSettings.email.startTLSEnabled)
    props.put("mail.smtp.host", JobSettings.email.host)
    props.put("mail.smtp.port", JobSettings.email.port.toString)
    props
  }

  private def emailSession () = {
    val props = emailProperties
    val session = Session.getInstance(props,
      new javax.mail.Authenticator() {
        protected override def getPasswordAuthentication(): PasswordAuthentication = {
          new PasswordAuthentication(JobSettings.email.username, JobSettings.email.password)
        }
      })
    session
  }

  private def emailMessage (s: Session, from: String,
                            to:String, subject: String, text:String) = {
    val message = new MimeMessage(s)
    message.setFrom(new InternetAddress(from))
    message.setRecipients(Message.RecipientType.TO, to.toLowerCase)
    message.setSubject(subject)
    message.setText(text)
    message
  }

  private def sendOrderInfoEmail (job: OrderInfoEmail) = {
    val session = emailSession
    try {
      val message = emailMessage(session, job.from,
        JobSettings.email.from,
        EmailTemplates.order_info.subject(job.order_id, job.from),
        EmailTemplates.order_info.body(job.order_id, job.from, job.info))
      Transport.send(message)
    } catch {
      case (e: MessagingException) => JobSettings.logger.warn("Error sending order information email from  '"
        + job.from + "'")
    }
  }

  private def sendOrderConfirmationEmail (job: OrderConfirmationEmail) = {
    val session = emailSession
    try {
      val message = emailMessage(session, JobSettings.email.from,
                      job.order.email, EmailTemplates.confirm_order.subject,
                      EmailTemplates.confirm_order.body(job.order.id.getOrElse(""),
                        "xxxx" + (job.order.credit_card.cc_number takeRight 4),
                        OrderValidator.formatPrice(job.order.total.getOrElse(0L))))
      Transport.send(message)
      JobSettings.logger.info("Sent order confirmation email to '" + job.order.email.toLowerCase + "'")
    } catch {
      case (e: MessagingException) => JobSettings.logger.warn("Error sending order confirmation email to  '"
        + job.order.email.toLowerCase + "'")
    }
  }

  private def sendRegistrationEmail (job: RegistrationEmail) = {
    val session = emailSession
    try {
      val message = emailMessage(session, JobSettings.email.from,
                      job.invite.user.email, EmailTemplates.registration.subject,
                      EmailTemplates.registration.body(job.invite.code))
      Transport.send(message)
      JobSettings.logger.info("Sent registration email to '" + job.invite.user.email.toLowerCase + "'")

    } catch {
      case (e: MessagingException) => JobSettings.logger.warn("\nError sending registration email to '"
        + job.invite.user.email.toLowerCase + "'")
    }
  }

  private def sendForgotPwEmail (job: ForgotPwEmail) = {
    val session = emailSession
    try {
      val message = emailMessage(session, JobSettings.email.from,
        job.credential.email, EmailTemplates.pw_reminder.subject,
        EmailTemplates.pw_reminder.body(job.credential.invite_code.getOrElse("")))
      Transport.send(message)
      JobSettings.logger.info("Sent password reminder email to '" + job.credential.email.toLowerCase + "'")

    } catch {
      case (e: MessagingException) => JobSettings.logger.warn("\nError sending password reminder email to '"
        + job.credential.email.toLowerCase + "'")
    }
  }

  def receive = {
    case (job: RegistrationEmail) => {
      sendRegistrationEmail(job)
      context.stop(self)
    }
    case (job: ForgotPwEmail) => {
      sendForgotPwEmail(job)
      context.stop(self)
    }
    case (job: OrderConfirmationEmail) => {
      sendOrderConfirmationEmail(job)
      context.stop(self)
    }
    case (job: OrderInfoEmail) => {
      sendOrderInfoEmail(job)
      context.stop(self)
    }
  }
}
