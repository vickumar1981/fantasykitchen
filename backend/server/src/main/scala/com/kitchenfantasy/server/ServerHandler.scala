package com.kitchenfantasy.server

import com.typesafe.config.Config

import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.kitchenfantasy.server.SerializationProvider;
import com.kitchenfantasy.server.api._

import javax.servlet.ServletInputStream

class ServerHandler(config: Config, handlers: Seq[RestGroup] = Nil) extends AbstractHandler {
  private lazy val services = {
    handlers flatMap { _.services }
  }

  def ++ (restGroup: RestGroup): ServerHandler = {
    new ServerHandler(config, handlers ++ List(restGroup))
  }

  def :: (restGroup: RestGroup): ServerHandler = {
    new ServerHandler(config, Seq(restGroup) ++ handlers)
  }

  def handle(target: String, baseRequest: server.Request, httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) {
    val request = createRequestObject(httpRequest)
    httpResponse.setContentType("application/json")
    services find { _.service.isDefinedAt(request) } match {
      case Some(service) =>
        // TODO handle exceptions thrown from service methods
        val response = service.service.apply(request)
        writeResponse(response, httpResponse)
        baseRequest.setHandled(true)
      case None =>
        httpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND)
        httpResponse.getWriter.write(jsonError("url not found"))
        baseRequest.setHandled(true)
    }
  }

  private def jsonError(error: String): String = s"""{ "error" : "$error" }"""

  private def writeResponse(response: Response, httpResponse: HttpServletResponse) {
    response match {
      case OK =>
        httpResponse.setStatus(HttpServletResponse.SC_OK)
        httpResponse.getWriter.write("{}")
      case Error(code, error) =>
        httpResponse.setStatus(code)
        httpResponse.getWriter.write(jsonError(error))
      case JSONResponse(data, rows) =>
        httpResponse.setStatus(HttpServletResponse.SC_OK)
        httpResponse.getWriter.write(SerializationProvider.write(response))
    }
  }

  private def createRequestObject(httpRequest: HttpServletRequest): Request = {

import org.apache.commons.io.IOUtils

    lazy val parseUrl = httpRequest.getRequestURI.split("/").toList.tail
    lazy val data = IOUtils.toString(httpRequest.getInputStream, "UTF-8")

    httpRequest.getMethod match {
      case "GET" => GET(parseUrl)
      case "POST" => POST(parseUrl, data)
      case "PUT" => PUT(parseUrl, data)
      case "DELETE" => DELETE(parseUrl)
    }
  }
}
