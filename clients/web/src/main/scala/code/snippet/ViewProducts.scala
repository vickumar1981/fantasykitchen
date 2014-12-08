package code
package snippet

import code.lib.client.ProductClient
import code.lib.service.CartViewer
import net.liftweb.http.{S, SHtml}
import scala.xml.NodeSeq

import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Noop

import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model.{Product, Order, OrderValidator}

class ViewProducts extends CartViewer {
  private def noProductsErrMsg = <b>There are no products.</b>
  private def noOrdersErrMsg = <b>There are no orders.</b>
  private def noProductsMsg = <b>Sorry, we were unable to find any products at this time.</b>

  private var contact_order_id = ""
  private var contact_order_info = ""

  def viewCart (in: NodeSeq): NodeSeq = renderCart(in)

  def viewOrderDetails (in: NodeSeq): NodeSeq = {
    if (showOrderDetails)
      renderOrderDetails(in)
    else NodeSeq.Empty
  }

  private def showProductItem (p: Product) =
    "#productImage [src]" #> p.imageUrl &
      "#productPrice *" #>  OrderValidator.formatPrice (p.price) &
      "#productDesc *" #> p.description &
      "#addToCart [onclick]" #> SHtml.ajaxInvoke (() => addProductToCart(p))

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

  def viewOrders (in: NodeSeq): NodeSeq = {
    if (showOrderDetails)
      NodeSeq.Empty
    else {
      val cssSel = ProductClient.viewOrders match {
        case Some(orderList) => {
          if (orderList.rows == 0)
            "#orderItem" #> noOrdersErrMsg &
              ".orderMenu [style+]" #> "display:none" &
              "#viewOrderDetails [style+]" #> "display:none"
          else
            "#orderRow *" #> orderList.data.map { o => showOrderItem(o)} &
              ".orderMenu [style!]" #> "display:none"
        }
        case _ => "#orderItem" #> noOrdersErrMsg &
                    ".orderMenu [style!]" #> "display:none" &
                    "#viewOrderDetails [style+]" #> "display:none"
      }
      cssSel(in)
    }
  }

  def sendOrderEmail = {
    def processOrderEmail: JsCmd = {
      if (contact_order_id.length > 0 && contact_order_info.length > 0) {
        ProductClient.sendOrderEmail(contact_order_id, contact_order_info) match {
          case Some(s) => {
            S.notice(renderNotice("Order email sent..."))
            S.redirectTo("/")
          }
          case _ => S.notice(renderNotice("The order id was not found. Please try again."))
        }
      }
      else S.notice(renderNotice("Please fill out the order id and comments fields."))
      Noop
    }

    "#order_id" #> SHtml.text(contact_order_id, contact_order_id = _) &
      "#contact_email" #> SHtml.textarea(contact_order_info, contact_order_info = _) &
      "#process_contact_email" #> (SHtml.hidden(() => processOrderEmail) )
  }
}