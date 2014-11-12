package com.kitchenfantasy.server

import com.basho.riak.client.IRiakClient

object DataProvider {
  private var clientContainer: Option[IRiakClient] = None    
  
  def configure(client: IRiakClient) {
    clientContainer = Some(client)
  }

  def client = clientContainer match {
    case Some(value) => value
    case None => throw new RuntimeException("DataProvider has not been configured yet.")
  }
}
