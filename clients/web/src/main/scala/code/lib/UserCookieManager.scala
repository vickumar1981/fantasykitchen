package code
package lib

import javax.crypto.{Cipher, SecretKey, SecretKeyFactory}
import javax.crypto.spec.{PBEKeySpec, PBEParameterSpec}
import sun.misc.{BASE64Decoder, BASE64Encoder}

trait UserCookieManager {
  lazy val userCookieName = "__kitchenfantasy__"
  lazy val cookieDuration = 2592000

  private lazy val PASSWORD: Array[Char] = "##fantasyKitchenWebApp".toCharArray()
  private lazy val SALT: Array[Byte] = Array (222.toByte, 51.toByte, 16.toByte, 18.toByte,
    222.toByte, 51.toByte, 16.toByte, 18.toByte)

  private def base64Encode(bytes: Array[Byte]): String = new BASE64Encoder().encode(bytes)
  private def base64Decode(data: String): Array[Byte] = new BASE64Decoder().decodeBuffer(data)

  def storeUserCookie (email: String, pw: String) = encryptCookie (email + "," + pw)

  def getUserCookie(data: String) = {
    var email = new StringBuilder
    var pw = new StringBuilder
    val cookieData: Array[String] = decryptCookie(data).split(",")
    if (cookieData.length > 1) {
      email ++= cookieData(0)
      for (i <- 1 to (cookieData.length - 1))
        pw ++= cookieData(i)
      (email.toString, pw.toString)
    }
    else ("", "")
  }

  private def encryptCookie(data: String): String = {
    val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
    val key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD))
    val pbeCipher = Cipher.getInstance("PBEWithMD5AndDES")
    pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20))
    base64Encode(pbeCipher.doFinal(data.getBytes("UTF-8")))
  }

  private def decryptCookie(data: String): String = {
    val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
    val key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD))
    val pbeCipher = Cipher.getInstance("PBEWithMD5AndDES")
    pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20))
    new String(pbeCipher.doFinal(base64Decode(data)), "UTF-8")
  }


}
