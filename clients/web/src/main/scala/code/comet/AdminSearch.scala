package code
package comet

import java.text.SimpleDateFormat
import java.util.Calendar

import code.lib.client.{ApiClient, UserClient}
import net.liftweb.http.js.{JsCmds, JsCmd}

import net.liftweb._
import http._
import net.liftweb.common.{Empty, Full}
import net.liftweb.util.Helpers._
import com.kitchenfantasy.model._

import scala.xml.NodeSeq

class AdminSearch extends CometActor with CometListener {
  private var showOrderDetails = false
  private var adminQuery: OrderQuery = OrderQuery()

  private lazy val orderStatusList: Map[String, String] =
    Map("-- All --" -> "", "Approved" -> "approved",
      "Completed" -> "completed", "Refund" -> "refund")
  private var order_search = ""
  private var order_start_dt = ""
  private var order_end_dt = ""
  private var order_status = ""

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
      val status = orderStatusList(order_status)
      val query = (if (startDt > endDt) OrderQuery(order_search, endDt, startDt, status)
        else OrderQuery(order_search, startDt, endDt, status))
      OrderService !
        UpdateAdminSearch(ApiClient.currentUser.is.openOrThrowException("no user").email,
          ApiClient.sessionId.is, query)
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
        "#process_order_search" #> (SHtml.hidden(() => processAdminSearch)) &
        "#order_status" #> SHtml.select(orderStatusList.toSeq.sortBy(_._2).map(o => (o._1 -> o._1)),
          orderStatusList.find(_._2 == adminQuery.status) match {
            case Some(s) => Full(s._1)
            case _ => Empty
          }, order_status = _)
    }
  }

  override def lowPriority = {
    case (update: UpdateOrderDetails) =>
      ApiClient.currentUser.is match {
        case Full(u) =>
          if ((u.email.equals(update.email)) &&
              (update.sessionId.equals(ApiClient.sessionId.is))) {
            showOrderDetails = update.order.isDefined
            reRender()
          }
        case _ =>
      }
    case (update: UpdateAdminSearch) =>
      ApiClient.currentUser.is match {
        case Full(u) =>
          if ((u.email.equals(update.email)) &&
            (update.sessionId.equals(ApiClient.sessionId.is)))
            adminQuery = update.query
        case _ =>
      }
    case _ =>
  }
}
