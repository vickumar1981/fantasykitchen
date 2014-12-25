package code.lib.client

import code.lib.client.ApiClient.currentUser
import code.lib.service.UserCookieManager
import dispatch.Defaults._
import dispatch._
import net.liftweb.common.{Loggable, Empty, Full}
import net.liftweb.json.{DefaultFormats, JsonParser}
import com.kitchenfantasy.model._

object UserClient extends UserCookieManager with Loggable {
  private implicit val formats = DefaultFormats

  def isLoggedIn (): Boolean = {
    if (currentUser.isDefined)
      currentUser.get match {
        case Full(u) => true
        case _ => false
      }
    else false
  }

  def isAdmin (): Boolean = {
    if (currentUser.isDefined)
      currentUser.get match {
        case Full(u) => u.is_admin
        case _ => false
      }
    else false
  }

  def getUserAddress (): Address = {
    val emptyAddress = Address("", "", "", "", "")
    if (currentUser.isDefined)
      currentUser.get match {
        case Full(u) => u.address.getOrElse(emptyAddress)
        case _ => emptyAddress
      }
    else emptyAddress
  }

  def getUserBillingInfo (): CCInfo = {
    val emptyCCInfo = CCInfo("", "", 0, 0, "", "")
    if (currentUser.isDefined)
      currentUser.get match {
        case Full(u) =>
          u.credit_cards match {
            case Some(cc_list) =>
              if (cc_list.size > 0)
                cc_list(0)
              else emptyCCInfo
            case _ => emptyCCInfo
          }
        case _ => emptyCCInfo
      }
    else emptyCCInfo
  }

  def updateUserInfo (a: Address, c: CCInfo): Option[ApiUser] = {
    if (currentUser.isDefined)
      currentUser.get match {
        case Full(u) => {
          val credential = UserCredential (u.email, u.password)
          val update = UserUpdate (credential, a, c)
          val result = Http(ApiClient.user.updateInfo(update) OK as.String).either
          result() match {
            case Right(content) => {
              logger.info("Updating user info '%s'".format(u.email))
              val updatedUser = JsonParser.parse(content).extract[ApiUser]
              ApiClient.currentUser.set(Full(updatedUser.data))
              Some(updatedUser)
            }
            case _  => None
          }
        }
        case _ => None
      }
    else None
  }

  def forgotPw (u: UserCredential): Option[ApiUser] = {
    val result = Http(ApiClient.user.forgotPw(u) OK as.String).either
    result() match {
      case Right(content) => {
        val u = JsonParser.parse(content).extract[ApiUser]
        Some(u)
      }
      case _  => None
    }
  }

  def registerUser (u: User): Option[ApiUser] = {
    val result = Http(ApiClient.user.register(u) OK as.String).either
    result() match {
      case Right(content) => {
        logger.info("Registering user '%s'".format(u.email))
        val registeredUser = JsonParser.parse(content).extract[ApiUser]
        Some(registeredUser)
      }
      case _  => None
    }
  }

  def loginUser (u: UserCredential): Option[ApiUser] = {
    val result = Http(ApiClient.user.login(u) OK as.String).either
    result() match {
      case Right(content) => {
        logger.info("Logging in '%s'".format(u.email))
        val loggedInUser = JsonParser.parse(content).extract[ApiUser]
        ApiClient.currentUser.set(Full(loggedInUser.data))
        Some(loggedInUser)
      }
      case _ => None
    }
  }

  def logoutUser () = ApiClient.currentUser.set(Empty)
}