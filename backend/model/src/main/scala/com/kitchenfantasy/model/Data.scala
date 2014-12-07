package com.kitchenfantasy.model

case class Product (id: String,
                     name: String,
                     description: String,
                     price: Long,
                     imageUrl: String,
                     active: Boolean = true,
                     qty: Option[Int] = None)

case class Address(line1: String,
                   line2: String,
                   city: String,
                   state: String,
                   postalCode: String,
                   country: Option[String] = Some("USA"),
                   notes: Option[String] = None)

case class Promo (id: String,
                   discount: Double,
                   email: Option[String] = None,
                   product_id: Option[String] = None,
                   used: Boolean = false)

case class CCInfo (cc_type: String, cc_number: String, cc_expiry_month: Int,
                    cc_expiry_year: Int, first_name: String, last_name: String,
                    primary: Boolean = false)

case class UserCredential (email: String, password: String)

case class UserUpdate (credential: UserCredential, address: Address, credit_card: CCInfo)

case class User (email: String, password: String, first_name: String, last_name: String, confirmed: Boolean = false,
                  is_admin: Boolean = false, address: Option[Address] = None, credit_cards: Option[List[CCInfo]] = None,
                  invite_code: Option[String] = None)

case class InviteCode (user: User, code: String)

case class Transaction (credential: UserCredential, order: List[Product])

case class Order (email: String, credit_card: CCInfo, address: Address,
                  order: List[Product], total: Option[Long] = None,
                  subtotal: Option[Long] = None, tax: Option[Long] = None,
                  promo: Option[Promo] = None, timestamp: Option[Long] = None,
                  id: Option[String] = None, transaction_id: Option[String] = None)
