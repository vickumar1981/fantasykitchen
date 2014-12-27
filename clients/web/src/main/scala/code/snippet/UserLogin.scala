package code
package snippet

import code.lib.client.{UserClient, ApiClient}
import code.lib.service.RenderMessages
import net.liftweb.common.Full
import net.liftweb.http.provider.HTTPCookie
import net.liftweb.http.{S, RequestVar, SHtml}
import net.liftweb.http.js.{JE, JsCmd}
import net.liftweb.http.js.JsCmds.{Script, Noop}
import net.liftweb.json.{DefaultFormats, JValue}

import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model._

class UserLogin extends RenderMessages {
  implicit val formats = DefaultFormats
  private lazy val pageUrl = "/login"

  private object registrationInfo extends RequestVar[(String, String, String, Boolean)]("", "", "", false)
  private object forgotPWInfo extends RequestVar[(String, String, Boolean)]("","",false)
  private object redirectUrl extends RequestVar[(Option[String])](None)

  private def loginRedirect = "/%s".format(redirectUrl.is.getOrElse(""))
  private def loginPageUrl = redirectUrl.is match {
    case Some(url) => "%s?whence=%s".format(pageUrl, url)
    case _ => pageUrl
  }

  private var login_email = forgotPWInfo.get._1
  private var login_pw = forgotPWInfo.get._2
  private var login_confirm_pw = ""
  private var login_invite_code = ""
  private var remember_me = false

  private var email = registrationInfo.get._1
  private var pw = registrationInfo.get._2
  private var confirm_pw = registrationInfo.get._3
  private var invite_code = ""

  def showErrors (errorList: Map[String,String]): Boolean = {
    if (!errorList.filter(!_._2.isEmpty).isEmpty) {
      errorList.foreach {
        case (error_id, error_value) => {
          if (!error_value.isEmpty)
            S.notice (renderNotice(error_value))
        }
        case _ => Noop }
      true
    }
    else false
  }

  def register = {
    def processRegister: JsCmd = {
      val register_user = (if (registrationInfo.get._4)
        User (email.toLowerCase, pw, "", "", false, false, None, None, Some(invite_code))
      else User (email, pw, "", ""))
      val errorList= (if (registrationInfo.get._4) Map.empty[String, String]
                      else LoginValidator.validateRegistration(register_user, pw, confirm_pw))

      if (!showErrors(errorList))
        UserClient.registerUser (register_user) match {
          case Some (u) =>
            u.data.invite_code match {
              case Some(invite_code) => {
                if (u.data.confirmed) {
                  ApiClient.currentUser.set(Full(u.data))
                  S.notice(renderNotice("Registering user..."))
                  S.redirectTo(loginRedirect)
                }
                else
                  S.notice(renderNotice("The invite code was invalid.  Please check your email."))
              }
              case None =>
                S.redirectTo(loginPageUrl, () => {
                  S.notice(renderNotice("An invite code was sent to your email.  " +
                    "Please check your email address"))
                  registrationInfo((email, pw, pw, true))
                })
            }
          case _ => { S.notice(renderNotice("This account is already registered.  Please choose a different email."))
            Noop
          }
        }
      Noop
    }

    def cancelRegister: JsCmd = {
      S.redirectTo(loginPageUrl, () => {
        S.notice(renderNotice("Clearing form..."))
        registrationInfo(("", "", "", false))
      })
      Noop
    }

    (if (registrationInfo.get._4)
      "#email" #> SHtml.text(email, email = _, "readonly" -> "readonly") &
        "#pw" #> SHtml.password(pw, pw = _, "readonly" -> "readonly") &
        "#confirm_pw" #> SHtml.password(confirm_pw, confirm_pw = _, "readonly" -> "readonly") &
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

  def renderLoginJs = {
    val credentialsJs =
      """
        |function currentCredentials() {
        |            return { email:  $('#login_email').val(), password: $('#login_pw').val() };
        |        }
      """.stripMargin
    redirectUrl.set(Some(S.param("whence").openOr("")))
    "#currentCredentials" #> Script(JE.JsRaw(credentialsJs).cmd)
  }

  def logIn = {
    def processLogIn: JsCmd = {
      if (!login_pw.isEmpty && !login_email.isEmpty)
        if (forgotPWInfo._3) {
          val errorList = LoginValidator.validateLogin(login_email, login_pw, login_confirm_pw, true)
          if (!showErrors(errorList))
            UserClient.forgotPw(UserCredential(login_email.toLowerCase, login_pw,
              Some(login_invite_code))) match {
              case Some(u) => {
                ApiClient.currentUser.set(Full(u.data))
                S.redirectTo(loginRedirect, () => {
                  S.notice(renderNotice("Your password was updated successfully."))
                })
              }
              case None => S.notice(renderNotice("The invite code was invalid.  Please check your email."))
            }
          else S.notice("The password is incorrect. Please make sure the passwords match.")
        }
        else
          UserClient.loginUser (UserCredential(login_email, login_pw)) match {
            case Some (u) => {
              if (remember_me) {
                val cookie = UserClient.storeUserCookie(login_email, login_pw)
                S.addCookie(HTTPCookie(UserClient.userCookieName, cookie).setMaxAge(2592000).setPath("/"))
              }
              else S.addCookie(HTTPCookie(UserClient.userCookieName, "").setMaxAge(0).setPath("/"))
              S.notice(renderNotice("Logging in..."))
              S.redirectTo(loginRedirect)
            }
            case _ => S.notice(renderNotice("There email and password are invalid.  Please try again."))
          }
      else S.notice (renderNotice("Please provide an email and password."))
      Noop
    }

    def forgotMyPassword (value: JValue) = {
      value.extractOpt[UserCredential] match {
        case Some(c) => {
          val errorList = LoginValidator.validateLogin(c.email, c.password, c.password)

          if (!showErrors(errorList))
            UserClient.forgotPw(c) match {
              case Some(u) => S.redirectTo(loginPageUrl, () => {
                S.notice(renderNotice("An invite code was sent to your email.  " +
                  "Please check your email address"))
                forgotPWInfo((c.email, c.password, true))
              })
              case None => S.notice(renderNotice("This user is not registered. Please register..."))
            }
        }
        case None => Noop
      }
      Noop
    }

    def cancelForgotPW: JsCmd = {
      S.redirectTo(loginPageUrl, () => {
        S.notice(renderNotice("Clearing form..."))
        forgotPWInfo(("", "", false))
      })
      Noop
    }

    (if (forgotPWInfo.get._3)
      "#login_email" #> SHtml.text(login_email, login_email = _, "readonly" -> "readonly") &
        "#login_pw" #> SHtml.password(login_pw, login_pw = _) &
        "#remember_me" #> SHtml.checkbox(remember_me, (resp) => remember_me = resp,
          "readonly" -> "readonly") &
        "#process_login" #> (SHtml.hidden(() => processLogIn)) &
        "#login_user *" #> "Update" &
        "#cancel_btn [style!]" #> "display:none" &
        "#cancel_btn [onclick]" #> SHtml.onEvent((s) => cancelForgotPW) &
        "#login_confirm_pw" #> SHtml.password(login_confirm_pw, login_confirm_pw = _) &
        "#login_invite_code" #> SHtml.password(login_invite_code, login_invite_code = _)
    else
      "#login_email" #> SHtml.text(login_email, login_email = _) &
        "#login_pw" #> SHtml.password(login_pw, login_pw = _) &
        "#remember_me" #> SHtml.checkbox(remember_me, (resp) => remember_me = resp) &
        "#process_login" #> (SHtml.hidden(() => processLogIn)) &
        "#login_user *" #> "Login" &
        "#cancel_btn [style+]" #> "display:none" &
        "#forgot_password [onclick]" #> SHtml.jsonCall( JE.Call("currentCredentials"), forgotMyPassword _ ) &
        "#login_confirm_pw" #> SHtml.password(login_confirm_pw, login_confirm_pw = _,
          "style" -> "display:none") &
        "#login_invite_code" #> SHtml.password(login_invite_code, login_invite_code = _,
          "style" -> "display:none"))
  }
}
