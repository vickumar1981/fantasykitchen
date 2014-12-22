package code
package comet

import code.lib.client.{ProductClient, UserClient, ApiClient}
import code.lib.service.CartViewer
import net.liftweb._
import http._
import net.liftweb.common.Full
import scala.xml.NodeSeq
import net.liftweb.util.Helpers._
import com.kitchenfantasy.model._

class ViewOrders extends CometActor with CometListener with CartViewer {
  private var showOrderList = true
  private var adminQuery = OrderQuery()

  def registerWith = OrderService
  private def noOrdersErrMsg = <b>There are no orders.</b>
  override def lifespan = Full (25 seconds)

  def render = {
    if ((showOrderDetails) || (!showOrderList))
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
            "#orderRow *" #> orderList.data.map { o => showOrderItem(o)} &
              ".orderMenu [style!]" #> "display:none"
        }
        case _ => "#orderItem" #> noOrdersErrMsg &
          ".orderMenu [style!]" #> "display:none" &
          "#viewOrderDetails [style+]" #> "display:none"
      }
    }
  }

  override def lowPriority = {
    case (o: Order) =>
      ApiClient.currentUser.get match {
        case Full(u) =>
          if (u.email.equals(o.email))
            reRender()
        case _ =>
      }
    case (email: String, sessionId: String, showList: Boolean) =>
      ApiClient.currentUser.get match {
        case Full(u) =>
          if ((u.email.equals(email)) &&
              (sessionId.equals(ApiClient.sessionId.get))) {
            showOrderList = showList
            reRender()
          }
        case _ =>
      }
    case (email: String, sessionId: String, query: OrderQuery) =>
      ApiClient.currentUser.get match {
        case Full(u) =>
          if ((u.email.equals(email)) &&
            (sessionId.equals(ApiClient.sessionId.get))) {
            adminQuery = query
            reRender()
          }
        case _ =>
      }
    case _ =>
  }
}
