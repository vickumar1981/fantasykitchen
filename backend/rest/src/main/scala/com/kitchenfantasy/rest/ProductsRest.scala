package com.kitchenfantasy.rest

import com.kitchenfantasy.datastore.Products
import com.kitchenfantasy.model.Product
import com.kitchenfantasy.server.api.Error
import com.kitchenfantasy.server.api.GET
import com.kitchenfantasy.server.api.JSONResponse
import com.kitchenfantasy.server.api.Request
import com.kitchenfantasy.server.api.Response
import com.kitchenfantasy.server.api.Rest

class ProductsRest extends Rest {
  def service: PartialFunction[Request, Response] = {
    case GET("products" :: Nil) => {
      val products = Products.findProducts
      JSONResponse(products.sortBy(p => (p.name)), products.size)
    }
  }
}
