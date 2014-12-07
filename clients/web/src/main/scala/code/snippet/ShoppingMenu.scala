package code
package snippet

import code.lib.client.{UserClient, ProductClient}
import net.liftweb.http.provider.HTTPCookie
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.{JsCmd, JsCmds}

import scala.xml.NodeSeq

import net.liftweb.util.Helpers.strToCssBindPromoter

class ShoppingMenu {

  private def processLogOut : JsCmd = {
    UserClient.logoutUser
    S.addCookie(HTTPCookie(UserClient.userCookieName, "").setMaxAge(0).setPath("/"))
    S.redirectTo("/")
  }

  private def showLoginText (s: String) = <span><i class='fa fa-lock'></i> {s}</span>

  def showMenu (in: NodeSeq): NodeSeq = {
    val cssSel = (
      if (UserClient.isLoggedIn)
        "#orders [style!]" #> "display: none" &
        "#login_logout [onclick]" #> SHtml.onEvent((s) => processLogOut) &
          "#login_logout *" #> showLoginText("Logout")
      else
        "#orders [style+]" #> "display: none" &
        "#login_logout [onclick]" #> SHtml.onEvent((s) => S.redirectTo("/login")) &
          "#login_logout *" #> showLoginText("Login")) &
      "#cart_count *" #> ProductClient.updateCartText
    cssSel(in)
  }
}