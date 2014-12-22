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
  private var adminQuery = OrderQuery()

  private var order_search = ""
  private var order_start_dt = ""
  private var order_end_dt = ""

  lazy val dtFormat = new SimpleDateFormat("MM/dd/yyyy")

  def registerWith = OrderService
  override def lifespan = Full (25 seconds)

  private def getDateFilters = {
    val cal = Calendar.getInstance
    val today = Calendar.getInstance.getTime
    cal.add(Calendar.DAY_OF_YEAR, -5)
    val fiveDaysAgo = cal.getTime
    (dtFormat.format(fiveDaysAgo), dtFormat.format(today))
  }

  lazy val datePicker = getDateFilters

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
      val startDt = extractDt(order_start_dt, System.currentTimeMillis - 432000000)
      val endDt = extractDt(order_end_dt, System.currentTimeMillis)
      val query = (if (startDt > endDt) OrderQuery(order_search, endDt, startDt)
      else OrderQuery(order_search, startDt, endDt))
      OrderService ! (ApiClient.currentUser.get.get.email, ApiClient.sessionId.get, query)
      JsCmds.Noop
    }

    if (showOrderDetails || !UserClient.isAdmin)
      NodeSeq.Empty
    else {
      "#order_search" #> SHtml.text(order_search, order_search = _) &
        "#order_start_date" #> SHtml.text(datePicker._1, order_start_dt = _) &
        "#order_end_date" #> SHtml.text(datePicker._2, order_end_dt = _) &
        "#process_order_search" #> (SHtml.hidden(() => processAdminSearch))
    }
  }

  override def lowPriority = {
    case (email: String, sessionId: String, o: Option[Order]) =>
      ApiClient.currentUser.get match {
        case Full(u) =>
          if ((u.email.equals(email)) &&
              (sessionId.equals(ApiClient.sessionId.get))) {
            showOrderDetails = !o.isEmpty
            reRender()
          }
        case _ =>
      }
    case _ =>
  }
}
