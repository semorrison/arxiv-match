package net.tqft.arxivmatch

import scala.slick.driver.MySQLDriver.simple._
import java.sql.Date
import java.sql.Timestamp

object SQL {

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
    val hotornot = TableQuery[HotOrNot]
  }
  def apply[A](closure: slick.driver.MySQLDriver.backend.Session => A): A = Database.forURL("jdbc:mysql://mysql.tqft.net/mathematicsliteratureproject?user=mathscinetbot&password=zytopex", driver = "com.mysql.jdbc.Driver") withSession closure

}