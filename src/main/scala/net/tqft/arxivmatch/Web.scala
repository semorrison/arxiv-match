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
    println("Started arxiv-match.")
  }
}

class ResolverService extends Service[HttpRequest, HttpResponse] {
  def apply(req: HttpRequest): Future[HttpResponse] = {
    val response = Response()

    val parameters = new QueryStringDecoder(req.getUri()).getParameters

    def getParameter(p: String) = {
      import scala.collection.JavaConverters._
      Option(parameters.get(p)).map(_.asScala.headOption).flatten
    }
    def getBooleanParameter(p: String) = getParameter(p).map(_.toLowerCase).flatMap({
      case "true" | "yes" => Some(true)
      case "false" | "no" => Some(false)
      case _ => None
    })
    def getIntParameter(p: String) = getParameter(p).flatMap({
      case Int(i) => Some(i)
      case _ => None
    })
    val callback = getParameter("callback")
    val arxivid = getParameter("arxivid")
    val MRNumber = getIntParameter("MRNumber")
    val `match` = getBooleanParameter("match")
    val name = getParameter("name")
    val comment = getParameter("comment")

    if (arxivid.nonEmpty && MRNumber.nonEmpty && `match`.nonEmpty) {
      Matches.report(arxivid.get, MRNumber.get, `match`.get, name, comment)
    }

    val next = Matches.matches.next

    response.setStatusCode(200)
    val json = {
      import argonaut._, Argonaut._
      next.asJson.spaces2
    }

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

object Int {
  def unapply(s: String): Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _: java.lang.NumberFormatException => None
  }
}