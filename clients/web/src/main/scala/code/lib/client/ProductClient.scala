package code.lib.client

import code.lib.client.ApiClient.{currentUser, myCart}
import dispatch.Defaults._
import dispatch._
import net.liftweb.common.{Empty, Full}
import net.liftweb.json.{DefaultFormats, JsonParser}
import scala.collection.mutable.MutableList
import com.kitchenfantasy.model._

object ProductClient {
  private implicit val formats = DefaultFormats

  def shoppingCartItems(): List[Product] =
    if (myCart.isDefined)
      myCart.get match {
        case Full(productList) => productList
        case _ => List.empty
      }
    else List.empty

  def updateCartText = {
    val cartItems = shoppingCartItems
    if (cartItems.size > 0) {
      val cartSize = cartItems.map { product => product.qty.getOrElse(0)}.sum
      "(" + cartSize + ")"
    } else ""
  }

  def viewOrders (): Option[ApiOrders] = {
    if (currentUser.isDefined)
      currentUser.get match {
        case Full(u) => {
          val credential = UserCredential(u.email, u.password)
          val result = Http(ApiClient.products.viewOrders(credential) OK as.String).either
          result() match {
            case Right(content) => {
              val orders = JsonParser.parse(content).extract[ApiOrders]
              Some(orders)
            }
            case _ => None
          }
        }
      }
    else None
  }

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

  def viewProducts(): Option[ApiProduct] = {
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