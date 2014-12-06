package code
package lib

import code.lib.ApiClient.currentUser
import code.lib.ApiClient.myCart
import dispatch._, Defaults._
import com.kitchenfantasy.model._
import net.liftweb.json.JsonParser
import net.liftweb.json.DefaultFormats
import net.liftweb.common.{Empty, Full}

import scala.collection.mutable.MutableList

object ProductClient {
  private implicit val formats = DefaultFormats

  def updateCartText = (if (myCart.isDefined) {
    myCart.get match {
      case Full(cart) => (if (cart.size > 0) {
        val cartSize = cart.map { product => product.qty.getOrElse(0)}.sum
        "(" + cartSize + ")"
      } else "")
      case _ => ""
    }
  }
  else "")

  def orderProducts(products: List[Product]): Option[ApiOrder] = {
    if (currentUser.isDefined)
      currentUser.get match {
        case Full(u) => {
          val credential = UserCredential (u.email, u.password)
          val transaction = Transaction (credential, products)
          val result = Http(ApiClient.products.order(transaction) OK as.String).either
          result() match {
            case Right(content) => {
              println ("\nOrdering products for user " + u.email + "\n")
              val updatedOrder = JsonParser.parse(content).extract[ApiOrder]
              myCart.set(Empty)
              Some(updatedOrder)
            }
            case _  => None
          }
        }
        case _ => None
      }
    else None
  }

  def viewProducts: Option[ApiProduct] = {
    val result = Http(ApiClient.products.view OK as.String).either
    result() match {
      case Right(content) => {
        val productList = JsonParser.parse(content).extract[ApiProduct]
        Some(productList)
      }
      case _ => None
    }
  }

  def deleteProductFromCart (p: Product) = {
    myCart.get match {
      case Full(cartItems) => {
        val newCart: MutableList[Product] = MutableList()
        for (product <- cartItems) {
          if (!(product.id.equals(p.id)))
            newCart += product
        }
        myCart.set(Full(newCart.toList))
      }
      case _ => myCart.set(Full(List.empty))
    }
    true
  }

  def addProductToCart (p: Product, qtyDelta: Int = 1) = {
    var newProduct = Product(p.id, p.name, p.description, p.price, p.imageUrl, true, Some(1))
    myCart.get match {
      case Full(cartItems) => {
        val newCart: MutableList[Product] = MutableList()
        for (product <- cartItems) {
          if (product.id.equals(p.id)) {
            val newQty = product.qty.getOrElse(0) + qtyDelta
            newProduct = Product(p.id, p.name, p.description, p.price, p.imageUrl,
              true, Some(if (newQty > 0) newQty else 0))
          }
          else
            newCart += product
        }
        newCart += newProduct
        myCart.set(Full(newCart.toList))
      }
      case _ => myCart.set(Full(List(newProduct)))
    }
    true
  }
}