package com.kitchenfantasy.jobs

import com.paypal.api.payments._
import collection.JavaConversions._
import com.paypal.core.rest.{APIContext, OAuthTokenCredential}

import com.kitchenfantasy.model.{Order, OrderValidator}

case class ProcessCCPayment (order: Order)

object CCPaymentJob {
  private def getApiContext = {
    val sdkConfig = Map("mode" -> JobSettings.payment.paypalMode)
    val paypalConfig = mapAsJavaMap(sdkConfig)
    val token = new OAuthTokenCredential(JobSettings.payment.paypalClientId, JobSettings.payment.paypalClientSecret,
      paypalConfig)
    val apiContext = new APIContext(token.getAccessToken)
    apiContext.setConfigurationMap(paypalConfig)
    apiContext
  }

  private def refundCCPayment(order:Order): Boolean = {
    try {
      val sale = new Sale()
      sale.setId(order.sale_id.getOrElse(""))
      val refund = new Refund()
      val amount = new Amount()
      amount.setCurrency("USD")
      amount.setTotal(OrderValidator.formatPrice(order.total.getOrElse(0)).replace("$", ""))
      refund.setAmount(amount)
      sale.refund(getApiContext, refund)
      true
    }
    catch {
      case (e: Exception) => false
    }
  }

  private def processCCPayment(order: Order): Option[(String, String)] = {
    try {
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

      val createdPayment = payment.create(getApiContext)
      if (createdPayment.getState.equals("approved"))
        Some(createdPayment.getId,
          createdPayment.getTransactions()(0).getRelatedResources()(0).getSale().getId)
      else
        None
    }
    catch { case (e: Exception) => None }
  }

  def processPayment (order: Order): Option[(String, String)] = processCCPayment(order)
  def refundPayment (order: Order): Boolean = refundCCPayment(order)
}
