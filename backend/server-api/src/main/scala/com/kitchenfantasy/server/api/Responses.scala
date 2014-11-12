package com.kitchenfantasy.server.api

sealed trait Response

case object OK extends Response
case class Error(statusCode: Int, description: String) extends Response
case class JSONResponse(data: Any, rows: Integer) extends Response