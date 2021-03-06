package code.lib.client

import dispatch._
import net.liftweb.common.{Box, Empty}
import net.liftweb.http.SessionVar
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonAST._
import net.liftweb.json.Printer._
import com.kitchenfantasy.model._
import java.util.UUID

case class ApiUser(data: User, rows: Integer)
case class ApiOK (data: String, rows: Integer)
case class ApiError(statusCode: Int, description: String)
case class ApiProduct(data: List[Product], rows: Integer)
case class ApiOrder(data: Order, rows: Integer)
case class ApiOrders(data: List[Order], rows: Integer)

object ApiClient {
  private implicit val formats = DefaultFormats
  private def getApiBaseUrl(): String = {
    try {
      val apiUrl = sys.env("API_URL")
      apiUrl
    }
    catch {
      case _: Throwable => "http://localhost:8080"
    }
  }
  private lazy val baseUrl: String = getApiBaseUrl()
  object currentUser extends SessionVar[Box[User]](Empty)
  object sessionId extends SessionVar[String](UUID.randomUUID().toString.replace("-",""))
  object myCart extends SessionVar[Box[List[Product]]](Empty)

  object user {
    def updateInfo(u: UserUpdate) = url(baseUrl + "/user/info").POST.setBody(compact(render(decompose(u))))
    def register(u: User) = url(baseUrl + "/user/register").POST.setBody(compact(render(decompose(u))))
    def login(u: UserCredential) = url(baseUrl + "/user/login").POST.setBody(compact(render(decompose(u))))
    def forgotPw (u: UserCredential) = url(baseUrl + "/user/forgot_pw").POST.setBody(compact(render(decompose(u))))
  }

  object products {
    def view = url(baseUrl + "/products").GET
    def order(t: Transaction) = url(baseUrl + "/products/order").POST.setBody(compact(render(decompose(t))))
    def updateOrderStatus(update: OrderUpdate) =
      url(baseUrl + "/products/order/status").POST.setBody(compact(render(decompose(update))))
    def viewOrders(u: UserCredential) = url(baseUrl + "/products/orders").POST.setBody(compact(render(decompose(u))))
    def adminQuery(search: OrderSearch) =
      url(baseUrl + "/products/orders/q").POST.setBody(compact(render(decompose(search))))
    def sendOrderEmail(u: OrderContactInfo) =
      url(baseUrl + "/products/order/email").POST.setBody(compact(render(decompose(u))))
  }
}