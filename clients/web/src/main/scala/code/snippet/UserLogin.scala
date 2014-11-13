package code
package snippet

import code.lib.UserClient
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Noop

import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model._
import org.mindrot.jbcrypt.BCrypt

class UserLogin {
  private var login_email = ""
  private var login_pw = ""
  private var remember_me = "no"

  private var email = ""
  private var pw = ""
  private var confirm_pw = ""

  private def renderNotice(msg: String) = <div class='register-req'><p>{msg}</p></div>

  def register = {

    def processRegister: JsCmd = {
      val register_user: User = User (email.toLowerCase, BCrypt.hashpw(pw, BCrypt.gensalt()), "", "")
      val errorList= LoginValidator.validateRegistration(register_user, pw, confirm_pw)

      if (!errorList.filter(!_._2.isEmpty).isEmpty) {
        errorList.foreach {
          case (error_id, error_value) => {
            S.notice (renderNotice(error_value))
          }
          case _ => Noop }
      }
      else {
        UserClient.registerUser (register_user) match {
          case Some (u) => {
            S.notice("Registering user...")
            S.redirectTo("/")
          }
          case _ => { S.notice(renderNotice("There was an error processing your request.  Please try again."))
            Noop
          }
        }
      }
      Noop
    }

    "#email" #> SHtml.text(email, email = _) &
      "#pw" #> SHtml.password(pw, pw = _) &
      "#confirm_pw" #> SHtml.password(confirm_pw, confirm_pw = _) &
      "#process_registration" #> (SHtml.hidden(() => processRegister) )

  }

  def logIn = {
    def processLogIn: JsCmd = {
      val login_user: UserCredential = UserCredential (login_email.toLowerCase, login_pw)
      UserClient.loginUser (login_user) match {
        case Some (u) => {
          S.notice(renderNotice("Logging in..."))
          S.redirectTo("/")
        }
        case _ => S.notice(renderNotice("There email and password are invalid.  Please try again."))
      }
      Noop
    }

    "#login_email" #> SHtml.text(login_email, login_email = _) &
      "#login_pw" #> SHtml.password(login_pw, login_pw = _) &
      "#process_login" #> (SHtml.hidden(() => processLogIn) )
  }
}
