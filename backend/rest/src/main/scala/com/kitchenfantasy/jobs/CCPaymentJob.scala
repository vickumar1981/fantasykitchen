package com.kitchenfantasy.jobs

import com.paypal.api.payments._

import collection.JavaConversions._

import akka.actor.Actor
import akka.actor.ActorSystem
import com.kitchenfantasy.server.GlobalConfiguration
import com.paypal.core.rest.{APIContext, OAuthTokenCredential}

import com.typesafe.config.{ConfigFactory, Config}

import com.kitchenfantasy.model.Order

import scala.collection.immutable.HashMap

case class ProcessCCPayment (order: Order)

class CCPaymentJob extends Actor {

  private def processCCPayment(order: Order) = {
    val sdkConfig = Map("mode" -> JobSettings.payment.paypalMode)
    val paypalConfig = mapAsJavaMap(sdkConfig).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    val token = new OAuthTokenCredential(JobSettings.payment.paypalClientId, JobSettings.payment.paypalClientId,
        paypalConfig)
    val apiContext = new APIContext("Bearer " + token.getAccessToken)
    apiContext.setConfigurationMap(paypalConfig)

    val creditCard = new CreditCard()
    creditCard.setType(order.credit_card.cc_type)
    creditCard.setNumber(order.credit_card.cc_number)
    creditCard.setExpireMonth(order.credit_card.cc_expiry_month)
    creditCard.setExpireYear(order.credit_card.cc_expiry_year)
    creditCard.setFirstName(order.credit_card.first_name)
    creditCard.setLastName(order.credit_card.last_name)

    val fundingInstrument = new FundingInstrument()
    fundingInstrument.setCreditCard(creditCard)

    val payer = new Payer()
    payer.setFundingInstruments(List(fundingInstrument))
    payer.setPaymentMethod("credit_card")

    val amount = new Amount()
    amount.setCurrency("USD")
    amount.setTotal((order.total.getOrElse(0L) * 0.01).toString)

    val transaction = new Transaction
    transaction.setDescription("creating a direct payment with credit card");
    transaction.setAmount(amount)

    val payment = new Payment()
    payment.setIntent("sale")
    payment.setPayer(payer)
    payment.setTransactions(List(transaction))

    val createdPayment = payment.create(apiContext)
  }

  def receive = {
    case (job: ProcessCCPayment) => {
      processCCPayment(job.order)
      context.stop(self)
    }
  }
}
