package code
package snippet

import code.lib.client.ProductClient
import code.lib.service.CartViewer
import net.liftweb.http.{S, SHtml}

import java.util.Date
import java.text.SimpleDateFormat
import scala.xml.NodeSeq

import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model.{Product, Order, OrderValidator}

class ViewProducts extends CartViewer {
  private def noProductsErrMsg = <b>There are no products.</b>
  private def noOrdersErrMsg = <b>There are no orders.</b>
  private def noProductsMsg = <b>Sorry, we were unable to find any products at this time.</b>

  private def defaultDateFormat = "MM-dd-yy HH:mm"

  def viewCart (in: NodeSeq): NodeSeq = renderCart(in)

  def viewOrderDetails (in: NodeSeq): NodeSeq = {
    if (showOrderDetails)
      renderOrderDetails(in)
    else NodeSeq.Empty
  }

  private def formatToDate (id: Long) = {
    val newDate = new Date(id)
    val df = new SimpleDateFormat(defaultDateFormat)
    df.format(newDate)
  }

  private def showProductItem (p: Product) =
    "#productImage [src]" #> p.imageUrl &
      "#productPrice *" #>  OrderValidator.formatPrice (p.price) &
      "#productDesc *" #> p.description &
      "#addToCart [onclick]" #> SHtml.ajaxInvoke (() => addProductToCart(p))

  private def showOrderItem (o: Order) =
    "#orderDate *" #> formatToDate(o.timestamp.getOrElse(0L)) &
      "#orderId *" #> o.id.getOrElse("---") &
      "#orderCC *" #> ("xxxx" + (o.credit_card.cc_number takeRight 4)) &
      "#orderTotal *" #> OrderValidator.formatPrice(o.total.getOrElse(0L)) &
      "#viewOrderDetails [onclick]" #> SHtml.ajaxInvoke(() =>
        S.redirectTo("/orders", () => viewOrderProductDetails(Some(o))))

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
            "#orderItem" #> noOrdersErrMsg
          else
            "#orderRow *" #> orderList.data.map { o => showOrderItem(o)}
        }
        case _ => "#orderItem" #> noOrdersErrMsg
      }
      cssSel(in)
    }
  }

}