package utils

import java.util.Date
import scala.util.Random

import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}

class JwtUtility {
  val JwtSecretKey = "secretKey"
  val JwtSecretAlgo = "HS256"

  def createToken(payload: String): String = {
    val randomString = payload + (new Date).toString + Random.nextInt(1000).toString
    val header = JwtHeader(JwtSecretAlgo)
    val claimsSet = JwtClaimsSet(randomString.slice(0, 255))

    JsonWebToken(header, claimsSet, JwtSecretKey)
  }

  def isValidToken(jwtToken: String): Boolean =
    JsonWebToken.validate(jwtToken, JwtSecretKey)

  def decodePayload(jwtToken: String): Option[String] =
    jwtToken match {
      case JsonWebToken(header, claimsSet, signature) => Option(claimsSet.asJsonString)
      case _ => None
    }
}

object JwtUtility extends JwtUtility
