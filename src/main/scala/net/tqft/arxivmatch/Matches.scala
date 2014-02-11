package net.tqft.arxivmatch

import scala.io.Source
import argonaut._
import Argonaut._
import scala.io.Codec
import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global

object Matches {
  import scala.slick.driver.MySQLDriver.simple._

  val articlesWithoutPublicationData = (for (
    a <- SQL.Tables.arxiv;
    if a.journalref.isNull;
    if a.doi.isNull;
    if !SQL.Tables.mathscinet_aux.filter(_.free === LiteralColumn("http://arxiv.org/abs/") ++ a.arxivid).exists
  ) yield (a.arxivid, a.title, a.authors)).sortBy(r => SQL.Tables.hotornot.filter(_.arxivid === r._1).length.asc)

  
  def articlesPage(k: Int) = synchronized {
    SQL { implicit session =>
      articlesWithoutPublicationData.drop(k * 100).take(100).list
    }
  }
  val articles = Iterator.continually(Iterator.from(0).map(articlesPage).takeWhile(_.nonEmpty).flatten).takeWhile(_.nonEmpty).flatten

  case class Match(arxivid: String, MRNumber: Int, bestURL: String)
  object Match {
    implicit def MatchCodecJson =
      casecodec3(Match.apply, Match.unapply)("arxivid", "MRNumber", "bestURL")
  }

  private val _matches = {
    case class Citation(MRNumber: Int, best: String)
    case class CitationScore(citation: Citation, score: Double)
    def searchQuery(title: String, authorsXML: String): List[CitationScore] = {
      val authors = (for (names <- (scala.xml.XML.loadString("<authors>" + authorsXML + "</authors>") \\ "author").iterator) yield (names \\ "keyname").text + ", " + (names \\ "forenames").text).mkString("", "; ", ";")
      val query = title + " - " + authors

      def encode(text: String) = {
        java.net.URLEncoder.encode(text, "UTF-8")
      }
      println("querying: " + query)
      val json = Source.fromURL("http://polar-dawn-1849.herokuapp.com/?q=" + encode(query))(Codec.UTF8).getLines.mkString("\n")
      println(json)

      case class Result(query: String, results: List[CitationScore])

      implicit def CitationCodecJson =
        casecodec2(Citation.apply, Citation.unapply)("MRNumber", "best")
      implicit def CitationScoreCodecJson =
        casecodec2(CitationScore.apply, CitationScore.unapply)("citation", "score")
      implicit def ResultCodecJson = casecodec2(Result.apply, Result.unapply)("query", "results")
      val results = json.decodeOption[Result].get.results
      println(results)
      results
    }

    for (
      (id, title, authorsXML) <- articles;
      CitationScore(citation, score) <- searchQuery(title, authorsXML) if score > 0.75;
      if !citation.best.startsWith("http://arxiv.org/abs/")
    ) yield Match(id, citation.MRNumber, citation.best)
  }

  private val matches = scala.collection.mutable.Queue[Match]()

  def next = {
    println(matches.size + " matches available in queue")
    val result = if (matches.nonEmpty) {
      matches.dequeue
    } else {
      _matches.next
    }
    if (matches.size < 10) {
      fill
    }
    result
  }

  def fill {
    future {
      for (_ <- 1 to 10) {
        matches.enqueue(_matches.next)
        println(matches.size + " matches available in queue")
      }
    }
  }

  def report(arxivid: String, MRNumber: Int, `match`: Boolean, name: Option[String], comment: Option[String]) {
    SQL { implicit session =>
      SQL.Tables.hotornot.map(_.content) += ((arxivid, MRNumber, `match`, name, comment))
    }
  }
}