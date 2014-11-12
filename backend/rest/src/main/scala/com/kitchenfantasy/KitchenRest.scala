package com.kitchenfantasy

import server.api.RestGroup
import com.kitchenfantasy.rest.ProductsRest

class KitchenRest extends RestGroup {
  val services = List(
    new ProductsRest
  )
}
