package com.kitchenfantasy.server.api

sealed trait Request

case class GET(url: List[String]) extends Request
case class POST(url: List[String], data: String) extends Request
case class PUT(url: List[String], data: String) extends Request
case class DELETE(url: List[String]) extends Request