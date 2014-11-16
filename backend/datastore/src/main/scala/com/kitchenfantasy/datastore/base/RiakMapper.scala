package com.kitchenfantasy.datastore.base

import java.util.UUID

import com.basho.riak.client.IRiakObject
import com.basho.riak.client.query.functions.JSSourceFunction
import com.basho.riak.client.query.indexes.BinIndex
import com.basho.riak.client.query.indexes.IntIndex
import com.basho.riak.client.raw.query.indexes.BinValueQuery
import com.basho.riak.client.raw.query.indexes.IntRangeQuery
import com.kitchenfantasy.server.DataProvider
import com.kitchenfantasy.server.SerializationProvider

class RiakMapper[T : Manifest](bucketName: String) {
  private val bucket = DataProvider.client.fetchBucket(bucketName).execute()
  private def readRiakValue(key: String): Option[(Array[Byte], String, Option[T])] = Option(bucket.fetch(key).execute()).map(raw => fromRiak(raw))
  
  def hasKey (key:String): Boolean = {
    readRiakValue (key) match {
      case Some (data) => true
      case None => false
    }
  }
  
  def read (key: String): Option[T] = {
    if (key.isEmpty)
      None
    else
	    readRiakValue (key) match {
	      case Some (data) => Some(SerializationProvider.fromStringToJson[T](data._2))
	      case None => None
	    }
  }
  
  def update(key: String, value: T) { bucket.store(key, toRiak(value)).execute() }

  def addIndex (key: String, idx: String, newVal: Long) = {
    val obj = bucket.fetch(key).execute()
    obj.removeIntIndex(idx).addIndex(idx, newVal)
    bucket.store(obj).execute()
  }  
  
  def addIndex (key: String, idx: String, newVal: String) = {
    val obj = bucket.fetch(key).execute()
    obj.removeBinIndex(idx).addIndex(idx, newVal)
    bucket.store(obj).execute()
  }

  private def convertToProduct = {
    new JSSourceFunction(
      "function(value, keyData, arg) {" +
        "var data = Riak.mapValuesJson(value)[0];" +
        "return [data];" +
        "}");
  }

  def findProducts = {
    val activeProducts = new BinValueQuery(BinIndex.named("active"), bucketName, "true")
    val results = DataProvider.client.mapReduce(activeProducts).addMapPhase(convertToProduct, true).execute().getResultRaw()
    SerializationProvider.fromStringToJson[List[T]](results)
  }
  
  def create (id: String, value: T): String = {
    bucket.store(id, toRiak(value)).execute()
    id
  }
  
  def create(value: T): String = {
    val id = generateId
    bucket.store(id, toRiak(value)).execute()
    id
  }
  
  def delete(key: String) { bucket.delete(key).execute() }

  protected def generateId = UUID.randomUUID().toString.replace("-","")

  protected def fromRiak(riakData: IRiakObject): (Array[Byte], String, Option[T]) = {
    SerializationProvider.read[T](riakData.getValue)
  }

  protected def toRiak(data: T): String = {
    SerializationProvider.write(data)
  }
}

