package code
package comet

import code.lib.client.ApiClient
import code.lib.service.CartViewer
import net.liftweb._
import http._
import net.liftweb.common.Full
import scala.xml.NodeSeq
import com.kitchenfantasy.model._

class ViewOrderDetails extends CometActor with CometListener with CartViewer {
  private var currentOrder: Option[Order] = None
  def registerWith = OrderService

  def render =
    if (currentOrder.isEmpty)
      NodeSeq.Empty
    else renderOrderDetails(currentOrder.get)

  override def lowPriority = {
    case (update: UpdateOrderDetails) =>
      ApiClient.currentUser.is match {
        case Full(u) =>
          if ((u.email.equals(update.email)) &&
            (update.sessionId.equals(ApiClient.sessionId.get))) {
            currentOrder = update.order
            reRender()
          }
        case _ =>
      }
    case _ =>
  }
}
