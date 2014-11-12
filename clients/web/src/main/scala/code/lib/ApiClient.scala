package code
package lib

import dispatch._, Defaults._
import com.kitchenfantasy.model._
import net.liftweb.json.JsonAST._
import net.liftweb.json.Extraction._
import net.liftweb.json.Printer._
import net.liftweb.json.DefaultFormats
import net.liftweb.http.SessionVar
import net.liftweb.common.Full
import net.liftweb.common.Box
import net.liftweb.common.Empty

case class ApiError(statusCode: Int, description: String)
case class ApiProduct(data: List[Product], rows: Integer)

object ApiClient {

  private implicit val formats = DefaultFormats

  private def baseUrl = "http://localhost:8080"

  object myCart extends SessionVar[Box[List[Product]]](Empty)

  def viewProducts = url(baseUrl + "/products/").GET
}