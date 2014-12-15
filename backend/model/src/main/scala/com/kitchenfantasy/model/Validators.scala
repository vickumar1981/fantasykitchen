package com.kitchenfantasy.model

import org.joda.time.format._
import org.mindrot.jbcrypt.BCrypt

object OrderValidator {
  def formatPrice (price: Long) = {
    val dollars = (price / 100)
    val cents = (price % 100)
    ("$" + dollars + "." + (if (cents < 10) "0" + cents else cents))
  }

  def orderTotals (order: List[Product]): (Long, Long, Long) = {
    val subtotal: Long = order.map { p => (p.price * p.qty.getOrElse(0)) }.sum
    val tax: Long = (subtotal * 5) / 100
    val total = subtotal + tax
    (total, subtotal, tax)
  }
}

object AddressValidator {
  private def isValidZipCode(zip: String): Boolean = zip.matches("^\\d{5}(?:[-\\s]\\d{4})?$")

  def validateAddress (addr: Address) = {
    val errors = scala.collection.mutable.Map[String,String]()
    if (addr.line1.isEmpty)
      errors += "addr" -> "Address is required."
    else
      errors += "addr" -> ""

    if (addr.city.isEmpty)
      errors += "city" -> "City is required."
    else
      errors += "city" -> ""

    if (addr.state.isEmpty)
      errors += "state" -> "State is required."
    else
      errors += "state" -> ""

    if (addr.postalCode.isEmpty)
      errors += "zip" -> "Zip code is required."
    else if (!addr.postalCode.isEmpty && !isValidZipCode(addr.postalCode) )
      errors += "zip" -> "Zip code is invalid."
    else
      errors += "zip" -> ""

    errors.toMap
  }
}

object CCValidator {
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

  def validateCC (cc: CCInfo) = {
    val errors = scala.collection.mutable.Map[String,String]()
    if (cc.cc_type.isEmpty)
      errors += "cc_type" -> "Credit card type is required."
    else
      errors += "cc_type" -> ""

    if (cc.cc_number.isEmpty)
      errors += "cc_no" -> "Credit card number is required."
    else if (!cc.cc_number.isEmpty && !checkIfCCIsValid(cc.cc_number.trim.replace("-", "")) )
      errors += "cc_no" -> "Credit card number is invalid."
    else
      errors += "cc_no" -> ""

    if (cc.cc_expiry_month == 0)
      errors += "cc_expiry_month" -> "Expiration month is required."
    else if (cc.cc_expiry_month < 0 || cc.cc_expiry_month > 12)
      errors += "cc_expiry_month" -> "Expiration month is invalid."
    else
      errors += "cc_expiry_month" -> ""

    if (cc.cc_expiry_year == 0)
      errors += "cc_expiry_year" -> "Expiration year is required."
    else
      errors += "cc_expiry_year" -> ""

    if (cc.last_name.isEmpty)
      errors += "cc_last_name" -> "Last name is required."
    else
      errors += "cc_last_name" -> ""

    if (cc.first_name.isEmpty)
      errors += "cc_first_name" -> "First name is required."
    else
      errors += "cc_first_name" -> ""
    errors.toMap
  }
}

object LoginValidator {
  private def minPWLength = 6

  private def isValidEmail(email: String): Boolean = """(.+)@([\w\.]+)""".r.unapplySeq(email).isDefined

  def encryptPW (pw: String) = BCrypt.hashpw(pw, BCrypt.gensalt())

  def checkIfPWMatch (pw1: String, pw2 :String) = {
    BCrypt.checkpw(pw2, pw1)
  }

  def validateLogin (e: String, pw: String, confirm_pw: String, checkPasswords: Boolean = false) = {
    val errors = scala.collection.mutable.Map[String,String]()

    if (e.isEmpty)
      errors += "email" -> "Email is required"
    else if (!e.isEmpty && !isValidEmail(e))
      errors += "email" -> "Email is invalid."
    else
      errors += "email" -> ""

    if (checkPasswords) {
      if (pw.isEmpty)
        errors += "pw" -> "Password is required."
      else if (pw.length < minPWLength)
        errors += "pw" -> ("Password must be at least " + minPWLength.toString + " characters.")
      else
        errors += "pw" -> ""

      if (confirm_pw.isEmpty)
        errors += "confirm_pw" -> "Password confirmation is required."
      else if (!pw.isEmpty && !pw.equals(confirm_pw))
        errors += "confirm_pw" -> "Passwords don't match"
      else
        errors += "confirm_pw" -> ""
    }
    errors.toMap
  }

  def validateRegistration (u: User, pw: String, confirm_pw: String) = {
    val errors = scala.collection.mutable.Map[String,String]()

    if (u.email.isEmpty)
      errors += "email" -> "Email is required"
    else if (!u.email.isEmpty && !isValidEmail(u.email))
      errors += "email" -> "Email is invalid."
    else
      errors += "email" -> ""

    if (pw.isEmpty)
      errors += "pw" -> "Password is required."
    else if (pw.length < minPWLength)
      errors += "pw" -> ("Password must be at least " + minPWLength.toString + " characters.")
    else
      errors += "pw" -> ""

    if (confirm_pw.isEmpty)
      errors += "confirm_pw" -> "Password confirmation is required."
    else if (!u.password.isEmpty && !u.password.equals(confirm_pw))
      errors += "confirm_pw" -> "Passwords don't match"
    else
      errors += "confirm_pw" -> ""

    errors.toMap
  }
}