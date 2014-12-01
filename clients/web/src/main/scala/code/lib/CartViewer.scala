package code
package lib

import net.liftweb.common.Full
import net.liftweb.http.js.JsCmd
import net.liftweb.http.{S, RequestVar, SHtml}
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.util.CssSel

import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model.Product


trait CartViewer {

  private def renderShoppingCart = SHtml.memoize { renderCart }

  private object shoppingCartTemplate extends RequestVar(renderShoppingCart)

  private def noProductsInCart = <b>There are no items in your cart.</b>

  private def showNoItemsInCart = "#cart_row *" #> noProductsInCart &
    ".cart_menu [style+]" #> "display:none" &
    "#order_summary [style+]" #> "display:none"

  protected def addProductToCart (p: Product): JsCmd = {
    ProductClient.addProductToCart(p)
    S.notice(<div class='register-req'><p>{p.name} added to cart.</p></div>)
    SetHtml("cart_count", <span>{ProductClient.updateCartText}</span>)
  }

  protected def formatPrice (price: Long) = {
    val dollars = (price / 100)
    val cents = (price % 100)
    ("$" + dollars + "." + (if (cents < 10) "0" + cents else cents))
  }

  private def showCartItem (p: Product): CssSel =
    "#cart_image [src]" #> p.imageUrl &
      "#cart_item_name *" #>  p.name &
      "#cart_item_price *" #> formatPrice (p.price) &
      ".cart_total_price *" #> formatPrice (p.price * p.qty.getOrElse(0)) &
      "#cart_item_desc *" #> p.description &
      ".cart_quantity_input [value]" #> p.qty.getOrElse(1) &
      ".cart_quantity_delete [onclick]" #> SHtml.ajaxInvoke(() => {
        ProductClient.deleteProductFromCart(p)
        SetHtml("cart_item", shoppingCartTemplate.is.applyAgain)
      }) &
      ".cart_quantity_up [onclick]" #> SHtml.ajaxInvoke(() => {
        ProductClient.addProductToCart(p)
        SetHtml("cart_item", shoppingCartTemplate.is.applyAgain)
      }) &
      ".cart_quantity_down [onclick]" #> SHtml.ajaxInvoke(() => {
        ProductClient.addProductToCart(p, -1)
        SetHtml("cart_item", shoppingCartTemplate.is.applyAgain)
      })

  private def showOrderSummary (order: List[Product]) = {
    val subtotal: Long = order.map { p => (p.price * p.qty.getOrElse(0)) }.sum
    val tax: Long = (subtotal * 5) / 100
    val total = subtotal + tax
    ".order_subtotal *" #> ("Subtotal: " + formatPrice(subtotal)) &
      ".order_tax *" #> ("Tax: " + formatPrice(tax)) &
      ".order_total *" #> ("Total: " + formatPrice(total))
  }

  protected def renderCart = ApiClient.myCart.get match {
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
}