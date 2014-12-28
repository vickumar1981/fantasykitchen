package code
package comet

import net.liftweb._
import http._
import actor._
import com.kitchenfantasy.model._

case class UpdateOrder (order: Order)
case class UpdateOrderStatus (email: String)
case class UpdateOrderDetails (email: String, sessionId: String, order: Option[Order])
case class UpdateAdminSearch (email: String, sessionId: String, query: OrderQuery)

object OrderService extends LiftActor with ListenerManager {
  var ordersUpdates = Vector[Order]()

  def createUpdate = ordersUpdates

  override def lowPriority = {
    case (update: UpdateOrder) => {
      ordersUpdates = ordersUpdates :+ update.order
      sendListenersMessage(update)
    }
    case (update: UpdateOrderStatus) => sendListenersMessage(update)
    case (update: UpdateOrderDetails) => sendListenersMessage(update)
    case (update: UpdateAdminSearch) => sendListenersMessage(update)
    case _ =>
  }
}