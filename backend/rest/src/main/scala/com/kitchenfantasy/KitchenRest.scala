package com.kitchenfantasy

import server.api.RestGroup
import com.kitchenfantasy.rest.{ProductsRest, UserRest}

class KitchenRest extends RestGroup {
  val services = List(
    new UserRest,
    new ProductsRest
  )
}
