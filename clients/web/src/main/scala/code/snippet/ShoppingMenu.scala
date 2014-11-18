package code
package snippet

import code.lib.{ApiClient, UserClient, ProductClient}
import net.liftweb.http.provider.HTTPCookie
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.{JsCmd, JsCmds}

import scala.xml.NodeSeq

import net.liftweb.util.Helpers.strToCssBindPromoter

class ShoppingMenu {

  private def processLogOut : JsCmd = {
    UserClient.logoutUser
    S.addCookie(HTTPCookie("__kitchenfantasy__", "").setMaxAge(0).setPath("/"))
    S.redirectTo("/")
  }

  private def showLoginText (s: String) = <a><i class='fa fa-lock'></i> {s}</a>

  def showMenu (in: NodeSeq): NodeSeq = {
    val cssSel = (
      if (ApiClient.isLoggedIn)
        "#login_logout [onclick]" #> SHtml.onEvent((s) => processLogOut) &
          "#login_logout *" #> showLoginText("Logout")
      else
        "#login_logout [onclick]" #> JsCmds.RedirectTo("/login") &
          "#login_logout *" #> showLoginText("Login")) &
      "#cart_count *" #> ProductClient.updateCartText
    cssSel(in)
  }
}