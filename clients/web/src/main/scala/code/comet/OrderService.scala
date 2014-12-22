package code
package comet

import net.liftweb._
import http._
import actor._
import com.kitchenfantasy.model._

object OrderService extends LiftActor with ListenerManager {
  var ordersUpdates = Vector[Order]()

  def createUpdate = ordersUpdates

  override def lowPriority = {
    case (o: Order) => {
      ordersUpdates = ordersUpdates :+ o
      updateListeners(o)
    }
    case (email: String, sessionId: String, showHeader: Boolean) =>
      updateListeners(email, sessionId, showHeader)
    case (email: String, sessionId: String, query: OrderQuery) =>
      updateListeners(email, sessionId, query)
    case _ =>
  }
}