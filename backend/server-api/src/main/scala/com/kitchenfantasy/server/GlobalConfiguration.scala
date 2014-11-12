package com.kitchenfantasy.server

import com.typesafe.config.{ConfigFactory, Config}
import java.util.concurrent.atomic.AtomicReference

object GlobalConfiguration {
  private val configHolder = new AtomicReference[Config](ConfigFactory.empty())

  def config = configHolder.get()

  def replace(newConfig: Config) {
    configHolder.set(newConfig)
  }
}
