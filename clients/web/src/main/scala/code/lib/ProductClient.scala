package code
package lib

import dispatch._, Defaults._
import com.kitchenfantasy.model._
import net.liftweb.json.JsonParser
import net.liftweb.json.DefaultFormats
import net.liftweb.common.Full
import net.liftweb.common.Empty

import scala.collection.mutable.MutableList

object ProductClient {
  private implicit val formats = DefaultFormats

  def viewProducts: Option[ApiProduct] = {
    val result = Http(ApiClient.viewProducts OK as.String).either
    result() match {
      case Right(content) => {
        val productList = JsonParser.parse(content).extract[ApiProduct]
        Some(productList)
      }
      case _ => None
    }
  }

  def deleteProductFromCart (p: Product) = {
    ApiClient.myCart.get match {
      case Full(cartItems) => {
        val newCart: MutableList[Product] = MutableList()
        for (product <- cartItems) {
          if (!(product.id.equals(p.id)))
            newCart += product
        }
        ApiClient.myCart.set(Full(newCart.toList))
      }
      case _ => ApiClient.myCart.set(Full(List.empty))
    }
    true
  }

  def addProductToCart (p: Product) = {
    var newProduct = Product(p.id, p.name, p.description, p.price, p.imageUrl, true, Some(1))
    ApiClient.myCart.get match {
      case Full(cartItems) => {
        val newCart: MutableList[Product] = MutableList()
        for (product <- cartItems) {
          if (product.id.equals(p.id))
            newProduct = Product(p.id, p.name, p.description, p.price, p.imageUrl,
              true, Some(product.qty.getOrElse(0) + 1))
          else
            newCart += product
        }
        newCart += newProduct
        ApiClient.myCart.set(Full(newCart.toList))
      }
      case _ => ApiClient.myCart.set(Full(List(newProduct)))
    }
    true
  }
}