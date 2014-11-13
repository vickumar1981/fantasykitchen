package code
package lib

import dispatch._, Defaults._
import com.kitchenfantasy.model._
import net.liftweb.json.JsonParser
import net.liftweb.json.DefaultFormats
import net.liftweb.common.Full
import net.liftweb.common.Empty

object UserClient {
  private implicit val formats = DefaultFormats

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