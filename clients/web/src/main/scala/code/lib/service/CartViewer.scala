package code.lib.service

import code.comet.{UpdateOrderDetails, UpdateOrder, OrderService}
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
  protected object shoppingCartTemplate extends RequestVar(renderShoppingCart)

  def showCheckout = !checkoutConfirmation.get
  def showConfirmation = checkoutConfirmation.get

  private def noProductsInCart = <b>There are no items in your cart.</b>

  private def formatToDate (id: Long) = {
    val newDate = new Date(id)
    val df = new SimpleDateFormat(defaultDateFormat)
    df.format(newDate)
  }

  private def showNoItemsInCart = ".cart_row *" #> noProductsInCart &
    ".cart_menu [style+]" #> "display:none" &
    "#order_summary [style+]" #> "display:none"

  protected def addProductToCart (p: Product): JsCmd = {
    ProductClient.addProductToCart(p)
    S.notice(<div class='register-req'><p>{p.name} added to
      <a href='/cart'><font color="FFFFFF"><u>Cart.</u></font></a></p></div>)
    SetHtml("cart_count", <span>{ProductClient.updateCartText}</span>)
  }

  private def showCartItem (p: Product, showOrderDetails: Boolean = false): CssSel =
    "#cart_image [src]" #> p.imageUrl &
      "#cart_item_name *" #>  p.name &
      "#cart_item_price *" #> OrderValidator.formatPrice (p.price) &
      ".cart_total_price *" #> OrderValidator.formatPrice (p.price * p.qty.getOrElse(0)) &
      "#cart_item_desc *" #> p.description &
      (if (showConfirmation || showOrderDetails)
        "#cart_quantity *" #> p.qty.getOrElse(0)
       else
        ".cart_quantity_input [value]" #> p.qty.getOrElse(0) ) &
      ".cart_quantity_delete [onclick]" #> SHtml.ajaxInvoke(() => {
        ProductClient.deleteProductFromCart(p)
        SetHtml("cart_count", <span>{ProductClient.updateCartText}</span>) &
        SetHtml("cart_content", shoppingCartTemplate.is.applyAgain)
      }) &
      ".cart_quantity_up [onclick]" #> SHtml.ajaxInvoke(() => {
        ProductClient.addProductToCart(p)
        SetHtml("cart_count", <span>{ProductClient.updateCartText}</span>) &
        SetHtml("cart_content", shoppingCartTemplate.is.applyAgain)
      }) &
      ".cart_quantity_down [onclick]" #> SHtml.ajaxInvoke(() => {
        ProductClient.addProductToCart(p, -1)
        SetHtml("cart_count", <span>{ProductClient.updateCartText}</span>) &
        SetHtml("cart_content", shoppingCartTemplate.is.applyAgain)
      })


  private def updateOrderStatus (order_id: String, status: String) (s: String) = {
    S.notice("Updating order...")
    ProductClient.updateOrderStatus(order_id, status) match {
      case Some(order) =>
        S.redirectTo ("/orders", ()=> {
          OrderService !
            UpdateOrderDetails(ApiClient.currentUser.is.openOrThrowException("no user").email,
              ApiClient.sessionId.is, None)
          S.notice(renderNotice("Updated order."))
        })
      case _ => S.notice(renderNotice("Error updating order."))
    }
    JsCmds.Noop
  }

  private def placeConfirmedOrder (order: List[Product]) (s: String) = {
    S.notice("Placing order...")
    ProductClient.orderProducts(order) match {
      case Some(order) => {
        OrderService ! UpdateOrder(order.data)
        S.redirectTo ("/orders", () => S.notice (renderNotice("Order successful.")))
      }
      case _ => S.redirectTo ("/checkout", () => S.notice(renderNotice("The credit card information is invalid.")))
    }
    JsCmds.Noop
  }

  private def showOrderSummary (order: List[Product],
                                showOrderDetails: Boolean = false,
                                 orderId: String = "", orderStatus: String = ""): CssSel = {
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
            S.redirectTo("/orders", () =>
              OrderService !
                UpdateOrderDetails(ApiClient.currentUser.is.openOrThrowException("no user").email,
                  ApiClient.sessionId.is, None))
            JsCmds.Noop })
         else
          SHtml.ajaxInvoke(()=> S.redirectTo("/checkout")))
      &
        (if (showOrderDetails && UserClient.isAdmin &&
          orderStatus.equalsIgnoreCase("approved"))
          "#adminButtons [style!]" #> "display:none" &
          "#completeOrder [onclick]" #>
            JsCmds.Confirm("Are you sure you want to complete this order?",
            SHtml.ajaxCall(JE.JsRaw("$('#adminButtons').hide()"),
              updateOrderStatus(orderId, "completed") _)) &
          "#refundOrder [onclick]" #>
            JsCmds.Confirm("Are you sure you want to refund this order?",
            SHtml.ajaxCall(JE.JsRaw("$('#adminButtons').hide()"),
              updateOrderStatus(orderId, "refund") _))
        else if (showOrderDetails && UserClient.isAdmin &&
          orderStatus.equalsIgnoreCase("completed"))
          "#adminButtons [style!]" #> "display:none" &
            "#completeOrder [style+]" #> "display:none" &
            "#refundOrder [onclick]" #>
              JsCmds.Confirm("Are you sure you want to refund this order?",
                SHtml.ajaxCall(JE.JsRaw("$('#adminButtons').hide()"),
                  updateOrderStatus(orderId, "refund") _))
        else
          "#adminButtons [style+]" #> "display:none")
    else "#checkout [style+]" #> "display:none" &
      "#adminButtons [style+]" #> "display:none")
  }

  protected def showOrderItem (o: Order, newOrders: List[String] = List.empty): CssSel =
      (if (newOrders.contains(o.id.getOrElse("")))
        ".orderRow [bgcolor+]" #> "#F0F0E9" &
        "#order-new-image [style]" #> "display:inline"
      else
        "#order-new-image [style]" #> "display:none") &
      "#orderDate *" #> formatToDate(o.timestamp.getOrElse(0L)) &
      "#orderStatus *" #> o.status.capitalize &
      "#orderId *" #> o.id.getOrElse("---") &
      "#orderCC *" #> ("xxxx" + (o.credit_card.cc_number takeRight 4)) &
      "#orderTotal *" #> OrderValidator.formatPrice(o.total.getOrElse(0L)) &
      "#viewOrderDetails [style!]" #> "display:none" &
      "#viewOrderDetails [onclick]" #> SHtml.ajaxInvoke(() => {
        OrderService !
          UpdateOrderDetails(ApiClient.currentUser.is.openOrThrowException("no user").email,
            ApiClient.sessionId.is, Some(o))
        JsCmds.Noop }) &
      "#orderName *" #> (o.credit_card.first_name + " " + o.credit_card.last_name) &
      "#orderEmail *" #> o.email &
      "#shippingAddress *" #> (o.address.line1 + ", " + o.address.line2) &
      "#shippingCity *" #> o.address.city &
      "#shippingState *" #> o.address.state &
      "#shippingZip *" #> o.address.postalCode &
      "#shippingNotes *" #> o.address.notes


  private def renderProductsList (productList: List[Product],
                                  orderInfo: Option[Order] = None): CssSel = {
    val (orderId: String, orderStatus: String) = orderInfo match {
      case Some(o) => (o.id.getOrElse(""), o.status)
      case _ => ("", "")
    }
    if (productList.size > 0) {
      val order = productList.sortBy(i => (i.name, i.id))
      ".cart_row" #> order.map { p => showCartItem(p, orderInfo.isDefined)} &
        ".cart_menu [style!]" #> "display:none" &
        "#order_summary [style!]" #> "display:none" &
        showOrderSummary(order, orderInfo.isDefined, orderId, orderStatus) &
        (if (orderInfo.isDefined) showOrderItem(orderInfo.get)
          else "orderItem [style+]" #> "display:none")
    }
    else showNoItemsInCart
  }

  protected def renderOrderDetails(o: Order): CssSel =
    renderProductsList(o.order, Some(o))
  protected def renderCart: CssSel = renderProductsList(ProductClient.shoppingCartItems)
}