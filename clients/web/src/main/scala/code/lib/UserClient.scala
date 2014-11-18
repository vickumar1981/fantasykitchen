package code
package lib

import dispatch._, Defaults._
import com.kitchenfantasy.model._
import net.liftweb.json.JsonParser
import net.liftweb.json.DefaultFormats
import net.liftweb.common.Full
import net.liftweb.common.Empty


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

object UserClient {
  private lazy val PASSWORD: Array[Char] = "##fantasyKitchenWebApp".toCharArray()
  private lazy val SALT: Array[Byte] = Array (222.toByte, 51.toByte, 16.toByte, 18.toByte,
    222.toByte, 51.toByte, 16.toByte, 18.toByte)

  private implicit val formats = DefaultFormats

  def storeUserCookie (email: String, pw: String) = encrypt (email + "," + pw)

  def getUserCookie(data: String) = {
    var email = new StringBuilder
    var pw = new StringBuilder
    val cookieData: Array[String] = decrypt(data).split(",")
    if (cookieData.length > 1) {
      email ++= cookieData(0)
      for (i <- 1 to (cookieData.length - 1))
        pw ++= cookieData(i)
      (email.toString, pw.toString)
    }
    else ("", "")
  }

  private def encrypt(data: String): String = {
    val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
    val key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD))
    val pbeCipher = Cipher.getInstance("PBEWithMD5AndDES")
    pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20))
    base64Encode(pbeCipher.doFinal(data.getBytes("UTF-8")))
  }

  private def decrypt(data: String): String = {
    val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
    val key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD))
    val pbeCipher = Cipher.getInstance("PBEWithMD5AndDES")
    pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20))
    new String(pbeCipher.doFinal(base64Decode(data)), "UTF-8")
  }

  private def base64Encode(bytes: Array[Byte]): String = new BASE64Encoder().encode(bytes)
  private def base64Decode(data: String): Array[Byte] = new BASE64Decoder().decodeBuffer(data)

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