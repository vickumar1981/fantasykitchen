package code
package lib

import dispatch._, Defaults._
import com.kitchenfantasy.model._
import net.liftweb.json.JsonParser
import net.liftweb.json.DefaultFormats
import net.liftweb.common.Full
import net.liftweb.common.Empty

object ProductClient {
  private implicit val formats = DefaultFormats

  def viewProducts: Option[ApiProduct] = {
    val result = Http(ApiClient.viewProducts OK as.String).either
    result() match {
      case Right(content) => {
        val productList = JsonParser.parse(content).extract[ApiProduct]
        Some(productList)
      }
      case _ => None
    }
  }
}