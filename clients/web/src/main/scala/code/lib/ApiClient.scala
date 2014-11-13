package code
package lib

import dispatch._, Defaults._
import com.kitchenfantasy.model._

import net.liftweb.common.{Box, Empty}
import net.liftweb.http.SessionVar
import net.liftweb.json.DefaultFormats

case class ApiError(statusCode: Int, description: String)
case class ApiProduct(data: List[Product], rows: Integer)

object ApiClient {

  private implicit val formats = DefaultFormats

  private def baseUrl = "http://localhost:8080"

  object myCart extends SessionVar[Box[List[Product]]](Empty)

  def viewProducts = url(baseUrl + "/products/").GET
}