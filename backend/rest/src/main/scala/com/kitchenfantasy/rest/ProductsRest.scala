package com.kitchenfantasy.rest

import com.kitchenfantasy.datastore.Products
import com.kitchenfantasy.server.api.{GET, JSONResponse, Request, Response, Rest}

class ProductsRest extends Rest {
  def service: PartialFunction[Request, Response] = {
    case GET("products" :: Nil) => {
      val products = Products.findProducts
      JSONResponse(products.sortBy(p => (p.name, p.id)), products.size)
    }
  }
}
