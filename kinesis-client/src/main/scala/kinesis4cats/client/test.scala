package kinesis4cats.client

import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse
import scala.jdk.CollectionConverters._
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest

object test {
    
  val req = PutRecordsRequest.builder().build()
  val reqRecords = req.records().asScala.toList


  PutRecordsResponse
    .builder()
    .build()
    .records()
    .asScala
    .toList
    .zipWithIndex
    .collect {
        case (entry, index) if Option(entry.errorCode()).nonEmpty => 
            val orig = reqRecords(index)
            
    }
}
