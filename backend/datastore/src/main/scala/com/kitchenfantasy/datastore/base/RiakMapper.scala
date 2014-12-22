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

  private def toJSData = new JSSourceFunction(
    """
      |function(value, keyData, arg) {
      | var data = Riak.mapValuesJson(value)[0];
      | return [data];
      |}
    """.stripMargin)

  def findByIndex (indexName: String, indexValue: String): List[T] = {
    val query = new BinValueQuery(BinIndex.named(indexName), bucketName, indexValue)
    val results = DataProvider.client.mapReduce(query).addMapPhase(toJSData, true).execute().getResultRaw()
    SerializationProvider.fromStringToJson[List[T]](results)
  }

  def findByIndex (indexName: String, startValue: Long, endValue: Long,
                   searchJs: Option[JSSourceFunction] = None): List[T] = {
    val query = new IntRangeQuery(IntIndex.named(indexName), bucketName, startValue, endValue)
    val results = searchJs match {
      case Some(func) =>
        DataProvider.client.mapReduce(query).addMapPhase(func, true).execute().getResultRaw()
      case None =>
        DataProvider.client.mapReduce(query).addMapPhase(toJSData, true).execute().getResultRaw()
    }
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

