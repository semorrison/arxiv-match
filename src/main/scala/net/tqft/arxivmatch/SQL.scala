package net.tqft.arxivmatch

import scala.slick.driver.MySQLDriver.simple._
import java.sql.Date
import java.sql.Timestamp

object SQL {

  class ArxivMathscinetMatches(tag: Tag) extends Table[(String, Int, Double, String)](tag, "arxiv_mathscinet_matches") {
    def arxivid = column[String]("arxivid")
    def MRNumber = column[Int]("MRNumber")
    def score = column[Double]("score")
    def best = column[String]("best")
    def * = (arxivid, MRNumber, score, best)
  }

  class MathscinetAux(tag: Tag) extends Table[(Int, String, String, String, String, Option[String], Option[String])](tag, "mathscinet_aux") {
    def MRNumber = column[Int]("MRNumber", O.PrimaryKey)
    def textTitle = column[String]("textTitle")
    def wikiTitle = column[String]("wikiTitle")
    def textAuthors = column[String]("textAuthors")
    def textCitation = column[String]("textCitation")
    def pdf = column[Option[String]]("pdf")
    def free = column[Option[String]]("free")
    def * = (MRNumber, textTitle, wikiTitle, textAuthors, textCitation, pdf, free)
    def citationData = (MRNumber, textTitle, wikiTitle, textAuthors, textCitation)
  }

  class Arxiv(tag: Tag) extends Table[(String, Date, Date, String, String, String, String, String, String, String, String, String, String, String, String)](tag, "arxiv") {
    def arxivid = column[String]("arxivid", O.PrimaryKey)
    def created = column[Date]("created")
    def updated = column[Date]("updated")
    def authors = column[String]("authors")
    def title = column[String]("title")
    def categories = column[String]("categories")
    def comments = column[String]("comments")
    def proxy = column[String]("proxy")
    def reportno = column[String]("reportno")
    def mscclass = column[String]("mscclass")
    def acmclass = column[String]("acmclass")
    def journalref = column[String]("journalref")
    def doi = column[String]("doi")
    def license = column[String]("license")
    def `abstract` = column[String]("abstract")
    def * = (arxivid, created, updated, authors, title, categories, comments, proxy, reportno, mscclass, acmclass, journalref, doi, license, `abstract`)
  }

  class HotOrNot(tag: Tag) extends Table[(String, Int, Boolean, Option[String], Option[String], Timestamp)](tag, "hotornot") {
    def arxivid = column[String]("arxivid")
    def MRNumber = column[Int]("MRNumber")
    def `match` = column[Boolean]("match")
    def name = column[Option[String]]("name")
    def comment = column[Option[String]]("comment")
    def timestamp = column[Timestamp]("timestamp")
    def content = (arxivid, MRNumber, `match`, name, comment)
    def * = (arxivid, MRNumber, `match`, name, comment, timestamp)
  }

  object Tables {
    val arxiv = TableQuery[Arxiv]
    val mathscinet_aux = TableQuery[MathscinetAux]
    val hotornot = TableQuery[HotOrNot]
    val arxiv_mathscinet_matches = TableQuery[ArxivMathscinetMatches]

  }
  def apply[A](closure: slick.driver.MySQLDriver.backend.Session => A): A = Database.forURL("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=mathscinetbot&password=zytopex", driver = "com.mysql.jdbc.Driver") withSession closure

}