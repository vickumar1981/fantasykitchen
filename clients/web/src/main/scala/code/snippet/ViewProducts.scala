package code
package snippet

import code.lib.{ApiClient, ProductClient}
import net.liftweb.common.Full
import net.liftweb.http.{RequestVar, SHtml, TransientRequestVar}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.util.CssSel

import net.liftweb.http.S
import scala.xml.NodeSeq

import com.kitchenfantasy.model._
import net.liftweb.util.Helpers.strToCssBindPromoter

class ViewProducts {
  private def noProductsErrMsg = <b>There are no products.</b>
  private def noProductsMsg = <b>Sorry, we were unable to find any products at this time.</b>

  private def noProductsInCart = <b>There are no items in your cart.</b>

  private def renderShoppingCart = SHtml.memoize { renderCart }

  private object shoppingCartTemplate extends RequestVar(renderShoppingCart)

  private object newProductAdded extends TransientRequestVar[Option[Product]](None)

  def showUpdateMessages (in: NodeSeq): NodeSeq =
    newProductAdded.is match {
      case Some(p) => {
        S.notice(<div class='register-req'><p>{p.name} added to cart.</p></div>)
        in
      }
      case _ => {
        S.notice("")
        NodeSeq.Empty
      }
    }

  def viewCart (in: NodeSeq): NodeSeq = renderCart(in)

  private def formatPrice (price: Long) = {
    if (price >= 100)
      ("$" + (price.toDouble * 0.01).toString.substring(0,price.toString.length + 1))
    else if (price < 10)
      ("$0.0" + price.toString)
    else
      ("$0." + price.toString)
  }

  private def addProductToCart (p: Product): JsCmd = {
    ProductClient.addProductToCart(p)
    JsCmds.RedirectTo("/cart", () => newProductAdded(Some(p)))
  }

  private def showCartItem (p: Product): CssSel =
    "#cart_image [src]" #> p.imageUrl &
      "#cart_item_name *" #>  p.name &
      "#cart_item_price *" #> formatPrice (p.price) &
      ".cart_total_price *" #> formatPrice (p.price * p.qty.getOrElse(0)) &
      "#cart_item_desc *" #> p.description &
      ".cart_quantity_input [value]" #> p.qty.getOrElse(1) &
      ".cart_quantity_delete [onclick]" #> SHtml.onEvent((s) => {
        ProductClient.deleteProductFromCart(p)
        SetHtml("cart_item", shoppingCartTemplate.is.applyAgain) & JsCmds.RedirectTo("/cart")
      }) &
      ".cart_quantity_up [onclick]" #> SHtml.onEvent((s) => {
        ProductClient.addProductToCart(p)
        SetHtml("cart_item", shoppingCartTemplate.is.applyAgain) & JsCmds.RedirectTo("/cart")
      }) &
      ".cart_quantity_down [onclick]" #> SHtml.onEvent((s) => {
        ProductClient.addProductToCart(p, -1)
        SetHtml("cart_item", shoppingCartTemplate.is.applyAgain) & JsCmds.RedirectTo("/cart")
      })

  private def showProductItem (p: Product) =
    "#productImage [src]" #> p.imageUrl &
      "#productPrice *" #>  formatPrice (p.price) &
      "#productDesc *" #> p.description &
      "#addToCart [onclick]" #> SHtml.ajaxInvoke (() => addProductToCart(p))

  private def showNoItemsInCart = "#cart_row *" #> noProductsInCart &
    ".cart_menu [style+]" #> "display:none" &
    "#order_summary [style+]" #> "display:none"

  private def showOrderSummary (order: List[Product]) = {
    val subtotal: Long = order.map { p => (p.price * p.qty.getOrElse(0)) }.sum
    val tax: Long = (subtotal * 5) / 100
    val total = subtotal + tax
    ".order_subtotal *" #> ("Subtotal: " + formatPrice(subtotal)) &
      ".order_tax *" #> ("Tax: " + formatPrice(tax)) &
      ".order_total *" #> ("Total: " + formatPrice(total))
  }

  private def renderCart = ApiClient.myCart.get match {
    case Full(productList) =>
      if (productList.size > 0) {
        val order = productList.sortBy(i => (i.name, i.id))
        "#cart_row *" #> order.map { p => showCartItem(p)} &
          ".cart_menu [style!]" #> "display:none" &
          "#order_summary [style!]" #> "display:none" &
          showOrderSummary (order)
      }
      else showNoItemsInCart
    case _ => showNoItemsInCart
  }

  def viewProducts (in: NodeSeq): NodeSeq = {
    val cssSel = ProductClient.viewProducts match {
      case Some(productList) => {
        if (productList.rows == 0)
          "#productItem" #> noProductsMsg
        else
          "#productItem *" #> productList.data.map { p => showProductItem(p) }
      }
      case _ => "#productItem" #> noProductsErrMsg
    }
    cssSel(in)
  }
}