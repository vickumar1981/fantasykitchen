package code.lib.service

import code.comet.OrderService
import code.lib.client.{ApiClient, UserClient, ProductClient}
import net.liftweb.http.js.{JE, JsCmd, JsCmds}
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.{SHtml, RequestVar, S}
import net.liftweb.util.CssSel
import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model.{Product, Order, OrderValidator}

import java.util.Date
import java.text.SimpleDateFormat

trait CartViewer extends RenderMessages {
  private def defaultDateFormat = "MM-dd-yy HH:mm"

  protected object checkoutConfirmation extends RequestVar[(Boolean)](false)
  private def renderShoppingCart = SHtml.memoize { renderCart }
  private object shoppingCartTemplate extends RequestVar(renderShoppingCart)

  def showCheckout = !checkoutConfirmation.get
  def showConfirmation = checkoutConfirmation.get

  private def noProductsInCart = <b>There are no items in your cart.</b>

  private def formatToDate (id: Long) = {
    val newDate = new Date(id)
    val df = new SimpleDateFormat(defaultDateFormat)
    df.format(newDate)
  }

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
      "name=cart_section [id+]" #> ( "product" + p.id ) &
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

  private def placeConfirmedOrder (order: List[Product]) (s: String) = {
    S.notice("Placing order...")
    ProductClient.orderProducts(order) match {
      case Some(order) => {
        S.redirectTo ("/orders", ()=> {
          OrderService ! order.data
          S.notice (renderNotice("Order successful."))
        })}
      case _ => {
        S.redirectTo ("/checkout", () => {
          S.notice(renderNotice("The credit card information is invalid."))
        })}
    }
    JsCmds.Noop
  }

  private def showOrderSummary (order: List[Product],
                                showOrderDetails: Boolean = false): CssSel = {
    val (total, subtotal, tax) = OrderValidator.orderTotals(order)
    ".order_subtotal *" #> ("Subtotal: " + OrderValidator.formatPrice(subtotal)) &
      ".order_tax *" #> ("Tax: " + OrderValidator.formatPrice(tax)) &
      ".order_total *" #> ("Total: " + OrderValidator.formatPrice(total)) &
    (if (UserClient.isLoggedIn)
      "#checkout [style!]" #> "display:none" &
      "#checkout [onclick]" #>
        (if (showConfirmation) SHtml.ajaxCall(JE.JsRaw("$('#checkout').hide()"), placeConfirmedOrder(order) _)
         else if (showOrderDetails)
          SHtml.ajaxInvoke(() => {
            OrderService ! (ApiClient.currentUser.get.get.email, ApiClient.sessionId.get, None)
            S.redirectTo("/orders")
            JsCmds.Noop })
         else
          SHtml.ajaxInvoke(()=> S.redirectTo("/checkout")))
    else "#checkout [style+]" #> "display:none")
  }

  protected def showOrderItem (o: Order): CssSel =
    "#orderDate *" #> formatToDate(o.timestamp.getOrElse(0L)) &
      "#orderId *" #> o.id.getOrElse("---") &
      "#orderCC *" #> ("xxxx" + (o.credit_card.cc_number takeRight 4)) &
      "#orderTotal *" #> OrderValidator.formatPrice(o.total.getOrElse(0L)) &
      "#viewOrderDetails [style!]" #> "display:none" &
      "#viewOrderDetails [onclick]" #> SHtml.ajaxInvoke(() => {
        OrderService ! (ApiClient.currentUser.get.get.email, ApiClient.sessionId.get, Some(o))
        JsCmds.Noop }) &
      "#orderName *" #> (o.credit_card.first_name + " " + o.credit_card.last_name) &
      "#orderEmail *" #> o.email


  private def renderProductsList (productList: List[Product],
                                  orderInfo: Option[Order] = None): CssSel = {
    if (productList.size > 0) {
      val order = productList.sortBy(i => (i.name, i.id))
      "#cart_row *" #> order.map { p => showCartItem(p)} &
        ".cart_menu [style!]" #> "display:none" &
        "#order_summary [style!]" #> "display:none" &
        showOrderSummary(order, orderInfo.isDefined) &
        (if (orderInfo.isDefined) showOrderItem(orderInfo.get)
          else "orderItem [style+]" #> "display:none")
    }
    else showNoItemsInCart
  }

  protected def renderOrderDetails(o: Order): CssSel =
    renderProductsList(o.order, Some(o))
  protected def renderCart: CssSel = renderProductsList(ProductClient.shoppingCartItems)
}