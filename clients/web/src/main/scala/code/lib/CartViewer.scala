package code
package lib

import net.liftweb.common.Full
import net.liftweb.http.js.JsCmd
import net.liftweb.http.{S, RequestVar, SHtml}
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.CssSel

import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model.{Product, OrderValidator}


trait CartViewer {

  protected object checkoutConfirmation extends RequestVar[(Boolean)](false)

  private def renderShoppingCart = SHtml.memoize { renderCart }
  private object shoppingCartTemplate extends RequestVar(renderShoppingCart)

  def showCheckout = !checkoutConfirmation.get
  def showConfirmation = checkoutConfirmation.get

  private def noProductsInCart = <b>There are no items in your cart.</b>

  private def showNoItemsInCart = "#cart_row *" #> noProductsInCart &
    ".cart_menu [style+]" #> "display:none" &
    "#order_summary [style+]" #> "display:none"

  protected def addProductToCart (p: Product): JsCmd = {
    ProductClient.addProductToCart(p)
    S.notice(<div class='register-req'><p>{p.name} added to cart.</p></div>)
    SetHtml("cart_count", <span>{ProductClient.updateCartText}</span>)
  }

  private def showCartItem (p: Product): CssSel =
    "#cart_image [src]" #> p.imageUrl &
      "#cart_item_name *" #>  p.name &
      "#cart_item_price *" #> OrderValidator.formatPrice (p.price) &
      ".cart_total_price *" #> OrderValidator.formatPrice (p.price * p.qty.getOrElse(0)) &
      "#cart_item_desc *" #> p.description &
      (if (showConfirmation)
        "#cart_quantity *" #> p.qty.getOrElse(1)
       else
        ".cart_quantity_input [value]" #> p.qty.getOrElse(1) ) &
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
    val (total, subtotal, tax) = OrderValidator.orderTotals(order)
    ".order_subtotal *" #> ("Subtotal: " + OrderValidator.formatPrice(subtotal)) &
      ".order_tax *" #> ("Tax: " + OrderValidator.formatPrice(tax)) &
      ".order_total *" #> ("Total: " + OrderValidator.formatPrice(total)) &
      "#checkout [onclick]" #> SHtml.onEvent((s)=> {
          if (showConfirmation) {
            S.notice("Placing order...")
            ProductClient.orderProducts(order) match {
              case Some(order) => {
                S.redirectTo ("/", ()=> {
                  S.notice ("Order successful.")
                })}
              case _ => {
                S.redirectTo ("/checkout", () => {
                  S.notice("The credit card information is invalid.")
                })}
            }
          }
          else {
            S.redirectTo("/checkout")
          }
        })
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