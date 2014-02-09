package net.tqft.arxivmatch

import org.jboss.netty.handler.codec.http.{ HttpRequest, HttpResponse }
import com.twitter.finagle.builder.ServerBuilder
import com.twitter.finagle.http.{ Http, Response }
import com.twitter.finagle.Service
import com.twitter.util.Future
import java.net.InetSocketAddress
import util.Properties
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import scala.io.Source
import scala.collection.JavaConverters.asScalaBufferConverter

object Web {
  def main(args: Array[String]) {
    val port = Properties.envOrElse("PORT", "8080").toInt
    println("Starting on port:" + port)
    ServerBuilder()
      .codec(Http())
      .name("arxivmatch")
      .bindTo(new InetSocketAddress(port))
      .build(new ResolverService)
    println("Started.")
  }
}

class ResolverService extends Service[HttpRequest, HttpResponse] {
  def apply(req: HttpRequest): Future[HttpResponse] = {
    val response = Response()

    val parameters = new QueryStringDecoder(req.getUri()).getParameters
    import scala.collection.JavaConverters._
    val callback = Option(parameters.get("callback")).map(_.asScala.headOption).flatten

    val next = None
    
    response.setStatusCode(200)
    val json = "{}"
    callback match {
      case Some(c) => {
        response.setContentType("application/javascript")
        response.contentString = c + "(" + json + ");"
      }
      case None => {
        response.setContentType("application/json")
        response.contentString = json
      }
    }

    Future(response)
  }
}