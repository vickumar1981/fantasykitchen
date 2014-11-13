package com.kitchenfantasy.model

import org.joda.time.format._
import org.mindrot.jbcrypt.BCrypt

object AddressValidator {
  private def isValidZipCode(zip: String): Boolean = zip.matches("^\\d{5}(?:[-\\s]\\d{4})?$")

  def validateAddress (addr: Address) = {
    val errors = scala.collection.mutable.Map[String,String]()
    if (addr.line1.isEmpty)
      errors += "billing_addr" -> "Billing address is required."
    else
      errors += "billing_addr" -> ""

    if (addr.city.isEmpty)
      errors += "billing_city" -> "Billing city is required."
    else
      errors += "billing_city" -> ""

    if (addr.state.isEmpty)
      errors += "billing_state" -> "Billing state is required."
    else
      errors += "billing_state" -> ""

    if (addr.postalCode.isEmpty)
      errors += "billing_zip" -> "Billing zip code is required."
    else if (!addr.postalCode.isEmpty && !isValidZipCode(addr.postalCode) )
      errors += "billing_zip" -> "Billing zip code is invalid."
    else
      errors += "billing_zip" -> ""

    errors
  }
}

object CCValidator {
  private val dateFormat = DateTimeFormat forPattern "MM/dd/yyyy"

  def toCCExpiryDate (cc_expiry: String): Long = {
    try {
      val results = cc_expiry.split('/')
      (results(2).toLong * 100) + results(0).toLong
    } catch {
      case e: Exception => 0
    }
  }

  def toCCSecurityCode (cc_code: String): Long = {
    try {
      cc_code.toLong
    } catch {
      case e: Exception => 0
    }
  }

  private def checkIfCCIsValid (cc_number: String) = {
    try {
      ( (cc_number.reverse.map { _.toString.toShort }.grouped(2) map {
        t => t(0) + (if (t.length > 1) (t(1) * 2) % 10 + t(1) / 5 else 0)
      }).sum % 10 == 0)
    }
    catch {
      case e: Exception => false
    }
  }

  private def checkDate(inputDate: String) = {
    val parseDate = try {
      Some(dateFormat parseDateTime inputDate)
    }
    catch {
      case e: IllegalArgumentException => None
    }
    parseDate match {
      case None => false
      case _ => true
    }
  }

  private def checkSecurityCode (cc_number: String, cc_code: String) = {
    try {
      val firstNumber = cc_code.charAt(0)
      if (cc_code.trim.matches("^\\d+$")) {
        if (firstNumber == '3')
          (cc_code.trim.matches("^\\d{4}$"))
        else
          (cc_code.trim.matches("^\\d{3}$"))
      }
      else false
    }
    catch {
      case e: Exception => false
    }
  }

  def validateCC (cc_number: String, cc_code: String, cc_expiry: String) = {
    val errors = scala.collection.mutable.Map[String,String]()
    if (cc_expiry.isEmpty)
      errors += "cc_expiry" -> "Credit card expiration date is required."
    else if (!cc_expiry.isEmpty && !checkDate(cc_expiry) )
      errors += "cc_expiry" -> "Credit card expiration date is invalid."
    else
      errors += "cc_expiry" -> ""

    if (cc_code.isEmpty)
      errors += "cc_code" -> "Credit card security code is required."
    else if (!cc_code.isEmpty && !checkSecurityCode(cc_number, cc_code))
      errors += "cc_code" -> "Credit card security code is invalid."
    else
      errors += "cc_code" -> ""

    if (cc_number.isEmpty)
      errors += "cc_no" -> "Credit card number is required."
    else if (!cc_number.isEmpty && !checkIfCCIsValid(cc_number.trim.replace("-", "")) )
      errors += "cc_no" -> "Credit card number is invalid."
    else
      errors += "cc_no" -> ""

    errors
  }
}

object LoginValidator {
  private def minPWLength = 6

  private def isValidEmail(email: String): Boolean = """(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined

  private def checkIfEmailsMatch (email1: String, email2: String) = {
    email1.equalsIgnoreCase(email2)
  }

  def checkIfPWMatch (pw1: String, pw2 :String) = {
    BCrypt.checkpw(pw2, pw1)
  }

  def validateRegistration (u: User, confirm_email: String, pw: String, confirm_pw: String) = {
    val errors = scala.collection.mutable.Map[String,String]()

    if (u.last_name.isEmpty)
      errors += "last_name" -> "Last name is required."
    else
      errors += "last_name" -> ""

    if (u.first_name.isEmpty)
      errors += "first_name" -> "First name is required."
    else
      errors += "first_name" -> ""

    if (u.email.isEmpty)
      errors += "email" -> "Email is required"
    else if (!u.email.isEmpty && !isValidEmail(u.email))
      errors += "email" -> "Email is invalid."
    else
      errors += "email" -> ""

    if (confirm_email.isEmpty)
      errors += "confirm_email" -> "Email confirmation is required."
    else if (!confirm_email.isEmpty && !checkIfEmailsMatch(u.email, confirm_email))
      errors += "confirm_email" -> "Emails don't match."
    else
      errors += "confirm_email" -> ""

    if (pw.isEmpty)
      errors += "pw" -> "Password is required."
    else if (pw.length < minPWLength)
      errors += "pw" -> ("Password must be at least " + minPWLength.toString + " characters.")
    else
      errors += "pw" -> ""

    if (confirm_pw.isEmpty)
      errors += "confirm_pw" -> "Password confirmation is required."
    else if (!u.password.isEmpty && !checkIfPWMatch(u.password, confirm_pw))
      errors += "confirm_pw" -> "Passwords don't match"
    else
      errors += "confirm_pw" -> ""

    //errors ++ AddressValidator.validateAddress(u.cc_address)
    errors
  }
}