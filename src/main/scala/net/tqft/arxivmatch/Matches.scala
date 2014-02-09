package net.tqft.arxivmatch

import scala.io.Source
import argonaut._
import Argonaut._
import scala.io.Codec

object Matches {
  import scala.slick.driver.MySQLDriver.simple._

  val articlesWithoutPublicationData = for (
    a <- SQL.Tables.arxiv;
    if a.journalref.isNull;
    if a.doi.isNull
  // TODO also remove everything in the correspondences table
  ) yield (a.arxivid, a.title, a.authors)

  def articlesPage(k: Int) = SQL { implicit session =>
    articlesWithoutPublicationData.drop(k * 100).take(100).list
  }
  val articles = Iterator.continually(Iterator.from(0).map(articlesPage).takeWhile(_.nonEmpty).flatten).takeWhile(_.nonEmpty).flatten

  case class Match(arxivid: String, MRNumber: Int, bestURL: String)
  object Match {
    implicit def MatchCodecJson =
      casecodec3(Match.apply, Match.unapply)("arxivid", "MRNumber", "bestURL")
  }

  val matches = {
    case class Citation(MRNumber: Int, best: String, score: Double)
    def searchQuery(title: String, authorsXML: String): List[Citation] = {
      val authors = (for (names <- (scala.xml.XML.loadString("<authors>" + authorsXML + "</authors>") \\ "author").iterator) yield (names \\ "keyname").text + ", " + (names \\ "forenames").text).mkString("", "; ", ";")
      val query = title + " - " + authors

      def encode(text: String) = {
        java.net.URLEncoder.encode(text, "UTF-8")
      }
      println("querying: " + query)
      val json = Source.fromURL("http://polar-dawn-1849.herokuapp.com/?q=" + encode(query))(Codec.UTF8).getLines.mkString("\n")
      println(json)

      case class Results(query: String, results: List[Citation])

      implicit def CitationCodecJson =
        casecodec3(Citation.apply, Citation.unapply)("MRNumber", "best", "score")
      implicit def ResultsCodecJson = casecodec2(Results.apply, Results.unapply)("query", "results")
      val results = json.decodeOption[Results].get.results
      println(results)
      results
    }

    for (
      (id, title, authorsXML) <- articles;
      citation <- searchQuery(title, authorsXML)
    //      if citation.score > 0.75
    ) yield Match(id, citation.MRNumber, citation.best)
  }

}