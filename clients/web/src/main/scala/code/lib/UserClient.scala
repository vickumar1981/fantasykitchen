package code
package lib

import code.lib.ApiClient.currentUser
import dispatch._, Defaults._
import com.kitchenfantasy.model._
import net.liftweb.json.{JsonParser, DefaultFormats}
import net.liftweb.common.{Full, Empty}

object UserClient extends UserCookieManager {
  private implicit val formats = DefaultFormats

  def updateUserInfo (a: Address, c: CCInfo): Option[ApiUser] = {
    if (currentUser.isDefined) {
      currentUser.get match {
        case Full(u) => {
          val credential = UserCredential (u.email, u.password)
          val update = UserUpdate (credential, a, c)
          val result = Http(ApiClient.updateUserInfo(update) OK as.String).either
          result() match {
            case Right(content) => {
              println ("\nUpdating user info " + u.email + "\n")
              val updatedUser = JsonParser.parse(content).extract[ApiUser]
              ApiClient.currentUser.set(Full(updatedUser.data))
              Some(updatedUser)
            }
            case _  => None
          }
        }
        case _ => None
      }
    }
    else None
  }

  def registerUser (u: User): Option[ApiUser] = {
    val result = Http(ApiClient.registerUser(u) OK as.String).either
    result() match {
      case Right(content) => {
        println ("\nRegistering user " + u.email + "\n")
        val registeredUser = JsonParser.parse(content).extract[ApiUser]
        ApiClient.currentUser.set(Full(registeredUser.data))
        Some(registeredUser)
      }
      case _  => None
    }
  }

  def loginUser (u: UserCredential): Option[ApiUser] = {
    val result = Http(ApiClient.loginUser(u) OK as.String).either
    result() match {
      case Right(content) => {
        println("\nLogging in " + u.email + "\n")
        val loggedInUser = JsonParser.parse(content).extract[ApiUser]
        ApiClient.currentUser.set(Full(loggedInUser.data))
        Some(loggedInUser)
      }
      case _ => None
    }
  }

  def logoutUser () = ApiClient.currentUser.set(Empty)
}