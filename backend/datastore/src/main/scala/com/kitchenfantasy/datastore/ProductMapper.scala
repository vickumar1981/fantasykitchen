package com.kitchenfantasy.datastore

import com.kitchenfantasy.datastore.base.RiakMapper
import com.kitchenfantasy.model.Product

object Products extends RiakMapper[Product]("kitchen-products") {

  private def addIndexes (id: String, product: Product) = {
    addIndex (id, "name", product.name)
    addIndex (id, "active", if (product.active) "true" else "false")
    id
  }

  def findProducts = findByIndex("active", "true")

  def createProduct (product: Product) {
    val id = create (product.id, product)
    addIndexes(id, product)
  }
  
  def updateProduct (id: String, product: Product) {
    update (id, product)
    addIndexes(id, product)
  }
}