package submission.messaging

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.AMQP.BasicProperties
import play.api.Logger
import play.Configuration

class Result

case object Success extends Result
case class  Failure(exception: Throwable) extends Result

/**
 * General Manager that holds connection properties to the RabbitMQ server as well as reusable properties of this connections
 */
object ConnectionManager {

  val durableProperties = new BasicProperties().builder().deliveryMode(2).build()

  val factory = {
    val f = new ConnectionFactory()
    val uri = readUri
    Logger.debug("Connection factory created with uri:"+uri)
    f.setUri(uri)
    f
  }

  /**
   * We will seek for a JVM param called ''override.rabbit.uri'' first, if not found
   * ''rabbit.uri'' it will be read from the config file and lastly if not found
   * ''mqp://localhost'' will be returned as the RabbitMQ server uri
   *@return URI of the rabbitmq server.
   */
  def readUri = {
    val uriParam = System.getProperty("override.rabbit.uri")
    if (uriParam != null && uriParam.trim.length > 0){
      Logger.debug("Found rabbit.uri from JVM params: "+uriParam)
      uriParam
    }else{
      val uri = Configuration.root().getString("rabbit.uri","amqp://localhost")
      Logger.debug("Using config uri: "+uri)
      uri
    }
  }
}

