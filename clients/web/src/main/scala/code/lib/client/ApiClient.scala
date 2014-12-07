package code.lib.client

import dispatch._
import net.liftweb.common.{Box, Empty}
import net.liftweb.http.SessionVar
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonAST._
import net.liftweb.json.Printer._
import com.kitchenfantasy.model._

case class ApiUser(data: User, rows: Integer)
case class ApiError(statusCode: Int, description: String)
case class ApiProduct(data: List[Product], rows: Integer)
case class ApiOrder(data: Order, rows: Integer)
case class ApiOrders(data: List[Order], rows: Integer)

object ApiClient {
  private implicit val formats = DefaultFormats
  private def baseUrl = "http://localhost:8080"
  object currentUser extends SessionVar[Box[User]](Empty)
  object myCart extends SessionVar[Box[List[Product]]](Empty)

  object user {
    def updateInfo(u: UserUpdate) = url(baseUrl + "/user/info").POST.setBody(compact(render(decompose(u))))
    def register(u: User) = url(baseUrl + "/user/register").POST.setBody(compact(render(decompose(u))))
    def login(u: UserCredential) = url(baseUrl + "/user/login").POST.setBody(compact(render(decompose(u))))
  }

  object products {
    def view = url(baseUrl + "/products").GET
    def order(t: Transaction) = url(baseUrl + "/products/order").POST.setBody(compact(render(decompose(t))))
    def viewOrders(u: UserCredential) = url(baseUrl + "/products/orders").POST.setBody(compact(render(decompose(u))))
  }
}