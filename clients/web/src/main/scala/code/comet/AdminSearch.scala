package code
package comet

import java.text.SimpleDateFormat
import java.util.Calendar

import code.lib.client.{ApiClient, UserClient}
import net.liftweb.http.js.{JsCmds, JsCmd}

import net.liftweb._
import http._
import net.liftweb.common.Full
import net.liftweb.util.Helpers._
import com.kitchenfantasy.model._

import scala.xml.NodeSeq

class AdminSearch extends CometActor with CometListener {
  private var showOrderDetails = false
  private var adminQuery: OrderQuery = OrderQuery()

  private var order_search = ""
  private var order_start_dt = ""
  private var order_end_dt = ""

  lazy val dtFormat = new SimpleDateFormat("MM/dd/yyyy")

  def registerWith = OrderService
  override def lifespan = Full (25 seconds)

  private def getDtFilter (in: Long) = {
    val cal = Calendar.getInstance
    cal.setTimeInMillis(in)
    dtFormat.format(cal.getTime)
  }

  private def extractDt (value: String, defaultVal: Long): Long = {
    try {
      val sdf = new SimpleDateFormat("MM/dd/yyyy")
      val newDt = sdf.parse(value)
      newDt.getTime
    }
    catch {
      case (e: Exception) => defaultVal
    }
  }

  def render = {
    def processAdminSearch: JsCmd = {
      val startDt = extractDt(order_start_dt, adminQuery.start_date)
      val endDt = extractDt(order_end_dt, adminQuery.end_date)
      val query = (if (startDt > endDt) OrderQuery(order_search, endDt, startDt)
        else OrderQuery(order_search, startDt, endDt))
      OrderService !
        UpdateAdminSearch(ApiClient.currentUser.get.get.email, ApiClient.sessionId.get, query)
      JsCmds.Noop
    }

    if (showOrderDetails || !UserClient.isAdmin)
      NodeSeq.Empty
    else {
      "#order_search" #> SHtml.text(adminQuery.text, order_search = _) &
        "#order_start_date" #> SHtml.text(getDtFilter(adminQuery.start_date),
          order_start_dt = _) &
        "#order_end_date" #> SHtml.text(getDtFilter(adminQuery.end_date),
          order_end_dt = _) &
        "#process_order_search" #> (SHtml.hidden(() => processAdminSearch))
    }
  }

  override def lowPriority = {
    case (update: UpdateOrderDetails) =>
      ApiClient.currentUser.get match {
        case Full(u) =>
          if ((u.email.equals(update.email)) &&
              (update.sessionId.equals(ApiClient.sessionId.get))) {
            showOrderDetails = update.order.isDefined
            reRender()
          }
        case _ =>
      }
    case (update: UpdateAdminSearch) =>
      ApiClient.currentUser.get match {
        case Full(u) =>
          if ((u.email.equals(update.email)) &&
            (update.sessionId.equals(ApiClient.sessionId.get)))
            adminQuery = update.query
        case _ =>
      }
    case _ =>
  }
}
