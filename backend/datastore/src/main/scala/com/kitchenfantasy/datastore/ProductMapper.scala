package com.kitchenfantasy.datastore

import com.kitchenfantasy.datastore.base.RiakMapper
import com.kitchenfantasy.model.Product

object Products extends RiakMapper[Product]("kitchen-products") {

  private def addIndexes (id: String, product: Product): Product = {
    addIndex (id, "name", product.name)
    addIndex (id, "active", if (product.active) "true" else "false")
    product
  }

  def findProducts(): List[Product] = findByIndex("active", "true")

  def createProduct (product: Product): Product = {
    val id = create (product.id, product)
    addIndexes(id, product)
  }
  
  def updateProduct (id: String, product: Product): Product = {
    update (id, product)
    addIndexes(id, product)
  }
}