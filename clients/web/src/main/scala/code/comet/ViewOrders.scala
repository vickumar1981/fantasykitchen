package code
package comet

import code.lib.client.{ProductClient, UserClient, ApiClient}
import code.lib.service.CartViewer
import net.liftweb._
import http._
import net.liftweb.common.Full
import scala.collection.mutable.ListBuffer
import scala.xml.NodeSeq
import net.liftweb.util.Helpers._
import com.kitchenfantasy.model._

class ViewOrders extends CometActor with CometListener with CartViewer {
  private var showOrderDetails = false
  private var adminQuery = OrderQuery()
  var newOrders = new ListBuffer[String]()

  def registerWith = OrderService
  private def noOrdersErrMsg = <b>There are no orders.</b>
  override def lifespan = Full (25 seconds)

  def render = {
    if (showOrderDetails)
      NodeSeq.Empty
    else {
      val orderList = (if (UserClient.isAdmin) ProductClient.adminSearch(adminQuery)
        else ProductClient.viewOrders)
      orderList match {
        case Some(orderList) => {
          if (orderList.rows == 0)
            "#orderItem" #> noOrdersErrMsg &
              ".orderMenu [style+]" #> "display:none" &
              "#viewOrderDetails [style+]" #> "display:none"
          else
            ".orderRow" #> orderList.data.map { o => showOrderItem(o, newOrders.toList)} &
              ".orderMenu [style!]" #> "display:none"
        }
        case _ => "#orderItem" #> noOrdersErrMsg &
          ".orderMenu [style!]" #> "display:none" &
          "#viewOrderDetails [style+]" #> "display:none"
      }
    }
  }

  override def lowPriority = {
    case (update: UpdateOrder) =>
      ApiClient.currentUser.is match {
        case Full(u) =>
          if (u.email.equals(update.order.email) || UserClient.isAdmin) {
            newOrders += update.order.id.getOrElse("")
            reRender()
          }
        case _ =>
      }
    case (update: UpdateOrderDetails) =>
      ApiClient.currentUser.is match {
        case Full(u) =>
          if ((u.email.equals(update.email)) &&
              (update.sessionId.equals(ApiClient.sessionId.get))) {
            showOrderDetails = update.order.isDefined
            if (showOrderDetails)
              newOrders -= update.order.get.id.getOrElse("")
            reRender()
          }
        case _ =>
      }
    case (update: UpdateAdminSearch) =>
      ApiClient.currentUser.is match {
        case Full(u) =>
          if ((u.email.equals(update.email)) &&
            (update.sessionId.equals(ApiClient.sessionId.get))) {
            adminQuery = update.query
            reRender()
          }
        case _ =>
      }
    case _ =>
  }
}
