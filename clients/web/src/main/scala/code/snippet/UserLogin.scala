package code
package snippet

import code.lib.{ApiClient, UserClient}
import net.liftweb.common.Full
import net.liftweb.http.provider.HTTPCookie
import net.liftweb.http.{S, RequestVar, SHtml}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Noop

import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model._
import org.mindrot.jbcrypt.BCrypt

class UserLogin {
  private lazy val pageUrl = "/login"
  private var login_email = ""
  private var login_pw = ""
  private var remember_me = false

  private object registrationInfo extends RequestVar[(String, String, String, Boolean)]("", "", "", false)

  private var email = registrationInfo.get._1
  private var pw = registrationInfo.get._2
  private var confirm_pw = registrationInfo.get._3
  private var invite_code = ""

  private def renderNotice(msg: String) = <div class='register-req'><p>{msg}</p></div>

  def register = {

    def processRegister: JsCmd = {
      val register_user: User = (if (registrationInfo.get._4)
        User (email.toLowerCase, pw, "", "", false, false, None, None, Some(invite_code))
        else User (email.toLowerCase, BCrypt.hashpw(pw, BCrypt.gensalt()), "", ""))
      val errorList= (if (registrationInfo.get._4) List.empty
                      else LoginValidator.validateRegistration(register_user, pw, confirm_pw))

      if (!errorList.filter(!_._2.isEmpty).isEmpty) {
        errorList.foreach {
          case (error_id, error_value) => {
            if (error_value.length > 0)
              S.notice (renderNotice(error_value))
          }
          case _ => Noop }
      }
      else {
        UserClient.registerUser (register_user) match {
          case Some (u) =>
            u.data.invite_code match {
              case Some(invite_code) => {
                if (u.data.confirmed) {
                  S.notice(renderNotice("Registering user..."))
                  S.redirectTo("/")
                }
                else
                  S.notice(renderNotice("The invite code was invalid.  Please check your email."))
              }
              case None => {
                ApiClient.currentUser.set(Full(u.data))
                S.redirectTo(pageUrl, () => {
                  S.notice(renderNotice("An invite code was sent to your email.  " +
                    "Please check your email address"))
                  registrationInfo((email, pw, confirm_pw, true))
                })
              }
            }
          case _ => { S.notice(renderNotice("This account is already registered.  Please choose a different email."))
            Noop
          }
        }
      }
      Noop
    }

    def cancelRegister: JsCmd = {
      S.redirectTo(pageUrl, () => {
        S.notice(renderNotice("Clearing form..."))
        registrationInfo(("", "", "", false))
      })
      Noop
    }

    (if (registrationInfo.get._4)
      "#email" #> SHtml.text(email, email = _,  "readonly" -> "readonly") &
        "#pw" #> SHtml.password(pw, pw = _,  "readonly" -> "readonly") &
        "#confirm_pw" #> SHtml.password(confirm_pw, confirm_pw = _,  "readonly" -> "readonly") &
        "#process_registration" #> (SHtml.hidden(() => processRegister)) &
        "#register_user *" #> "Verify" &
        "#cancel_btn [style!]" #> "display:none" &
        "#cancel_btn [onclick]" #> SHtml.onEvent((s) => cancelRegister) &
        "#invite_code" #> SHtml.password(invite_code, invite_code = _)
    else
      "#email" #> SHtml.text(email, email = _) &
        "#pw" #> SHtml.password(pw, pw = _) &
        "#confirm_pw" #> SHtml.password(confirm_pw, confirm_pw = _) &
        "#process_registration" #> (SHtml.hidden(() => processRegister)) &
        "#register_user *" #> "Signup" &
        "#cancel_btn [style+]" #> "display:none" &
        "#invite_code" #> SHtml.password(invite_code, invite_code = _, "style" -> "display:none"))
  }

  def logIn = {
    def processLogIn: JsCmd = {
      val login_user: UserCredential = UserCredential (login_email.toLowerCase, login_pw)
      UserClient.loginUser (login_user) match {
        case Some (u) => {
          if (remember_me) {
            S.addCookie(HTTPCookie("__kitchenfantasy_login", login_email).setMaxAge(2592000).setPath("/"))
            S.addCookie(HTTPCookie("__kitchenfantasy_pw", login_pw).setMaxAge(2592000).setPath("/"))
          }
          else {
            S.addCookie(HTTPCookie("__kitchenfantasy_login", "").setMaxAge(0).setPath("/"))
            S.addCookie(HTTPCookie("__kitchenfantasy_pw", "").setMaxAge(0).setPath("/"))
          }
          S.notice(renderNotice("Logging in..."))
          S.redirectTo("/")
        }
        case _ => S.notice(renderNotice("There email and password are invalid.  Please try again."))
      }
      Noop
    }

    "#login_email" #> SHtml.text(login_email, login_email = _) &
      "#login_pw" #> SHtml.password(login_pw, login_pw = _) &
      "#remember_me" #> SHtml.checkbox(remember_me, (resp) => remember_me = resp) &
      "#process_login" #> (SHtml.hidden(() => processLogIn) )
  }
}
