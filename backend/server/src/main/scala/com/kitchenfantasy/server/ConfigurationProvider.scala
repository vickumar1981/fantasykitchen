package com.kitchenfantasy.server

import com.kitchenfantasy.server.GlobalConfiguration;

trait ConfigurationProvider {
  def config = GlobalConfiguration.config
}
