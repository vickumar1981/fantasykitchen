package code
package lib

import dispatch._, Defaults._
import com.kitchenfantasy.model._

import net.liftweb.common.{Box, Empty}
import net.liftweb.http.SessionVar
import net.liftweb.json.DefaultFormats

import net.liftweb.json.JsonAST._
import net.liftweb.json.Extraction._
import net.liftweb.json.Printer._

case class ApiUser(data: User, rows: Integer)
case class ApiError(statusCode: Int, description: String)
case class ApiProduct(data: List[Product], rows: Integer)

object ApiClient {

  private implicit val formats = DefaultFormats

  private def baseUrl = "http://localhost:8080"

  object currentUser extends SessionVar[Box[User]](Empty)

  object myCart extends SessionVar[Box[List[Product]]](Empty)

  def isLoggedIn (): Boolean = {
    if (currentUser.isDefined) {
      val user = currentUser.get
      if (!user.isEmpty)
        user.open_!.confirmed
      else false
    }
    else false
  }

  def registerUser(u: User) = url(baseUrl + "/user/register").POST.setBody(compact(render(decompose(u))))

  def loginUser(u: UserCredential) = url(baseUrl + "/user/login").POST.setBody(compact(render(decompose(u))))

  def viewProducts = url(baseUrl + "/products/").GET
}