package code
package snippet

import code.comet.OrderService
import code.lib.client.{ApiClient, UserClient, ProductClient}
import code.lib.service.CartViewer
import net.liftweb.http.{S, SHtml}
import scala.xml.NodeSeq

import net.liftweb.http.js.{JsCmds, JE, JsCmd}
import net.liftweb.http.js.JsCmds.{Script, Noop}

import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model.{Product, Order, OrderValidator, OrderQuery}

import java.text.SimpleDateFormat
import java.util.Calendar

class ViewProducts extends CartViewer {
  private def noProductsErrMsg = <b>There are no products.</b>
  private def noProductsMsg = <b>Sorry, we were unable to find any products at this time.</b>

  lazy val dtFormat = new SimpleDateFormat("MM/dd/yyyy")

  private def getDateFilters = {
    val cal = Calendar.getInstance
    val today = Calendar.getInstance.getTime
    cal.add(Calendar.DAY_OF_YEAR, -5)
    val fiveDaysAgo = cal.getTime
    (dtFormat.format(fiveDaysAgo), dtFormat.format(today))
  }

  private var contact_order_id = ""
  private var contact_order_info = ""

  private var order_search = ""
  private var order_start_dt = ""
  private var order_end_dt = ""

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

  def renderAdminDatePickersJs = {
    val datePickersJs =
      """
        |$(function() {
        |    $( "#order_start_date" ).datepicker();
        |    $( "#order_end_date" ).datepicker();
        |  });
      """.stripMargin

    "#adminDatePickers" #> Script(JE.JsRaw(datePickersJs).cmd)
  }

  def adminSearch = {
    lazy val datePicker = getDateFilters

    def extractDt (value: String, defaultVal: Long): Long = {
      try {
        val sdf = new SimpleDateFormat("MM/dd/yyyy")
        val newDt = sdf.parse(value)
        newDt.getTime
      }
      catch {
        case (e: Exception) => defaultVal
      }
    }

    def processAdminSearch: JsCmd = {
      val startDt = extractDt(order_start_dt, System.currentTimeMillis - 432000000)
      val endDt = extractDt(order_end_dt, System.currentTimeMillis)
      val query = (if (startDt > endDt) OrderQuery(order_search, endDt, startDt)
        else OrderQuery(order_search, startDt, endDt))
      OrderService ! (ApiClient.currentUser.get.get.email, ApiClient.sessionId.get, query)
      JsCmds.Noop
    }

    if (UserClient.isAdmin)
      "#order_search" #> SHtml.text(order_search, order_search = _) &
        "#order_start_date" #> SHtml.text(datePicker._1, order_start_dt = _) &
        "#order_end_date" #> SHtml.text(datePicker._2, order_end_dt = _) &
        "#process_order_search" #> (SHtml.hidden(() => processAdminSearch))
    else NodeSeq.Empty
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