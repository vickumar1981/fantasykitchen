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

class ViewOrderDetails extends CometActor with CometListener with CartViewer {
  private var currentOrder: Option[Order] = None
  def registerWith = OrderService

  def render =
    if (currentOrder.isEmpty)
      NodeSeq.Empty
    else renderOrderDetails(currentOrder.get)

  override def lowPriority = {
    case (email: String, sessionId: String, o: Option[Order]) =>
      ApiClient.currentUser.get match {
        case Full(u) =>
          if ((u.email.equals(email)) &&
            (sessionId.equals(ApiClient.sessionId.get))) {
            currentOrder = o
            reRender()
          }
        case _ =>
      }
    case _ =>
  }
}
