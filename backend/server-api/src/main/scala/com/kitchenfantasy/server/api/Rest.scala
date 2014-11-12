package com.kitchenfantasy.server.api

trait Rest {
  def service: PartialFunction[Request, Response]
  protected def defaultRange:Long = 25000
}

trait RestGroup {
  val services: List[Rest]
}
