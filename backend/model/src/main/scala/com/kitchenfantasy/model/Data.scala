package com.kitchenfantasy.model

case class Product (id: String,
                     name: String,
                     description: String,
                     price: Long,
                     imageUrl: String,
                     active: Boolean,
                     qty: Option[Int] = None)

case class Address(line1: String,
                   line2: String,
                   city: String,
                   state: String,
                   postalCode: String,
                   country: String)

case class Promo (id: String,
                   discount: Double,
                   email: Option[String] = None,
                   product_id: Option[String] = None,
                   used: Boolean)

case class CCInfo (cc_type: String, cc_number: String, cc_expiry_month: Int,
                    cc_expiry_year: Int, first_name: String, last_name: String,
                    primary: Boolean)

case class UserCredential (email: String, password: String)

case class User (email: String, password: String, first_name: String, last_name: String,
                  address: Option[Address] = None, credit_cards: Option[List[CCInfo]] = None)

case class Order (id: String, email: String, credit_card: CCInfo, address: Address,
                  order: List[Product], tax_rate: Double, total: Option[Double] = None,
                  promo: Option[Promo] = None)
