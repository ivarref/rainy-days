import java.io.{File, PrintWriter}
import java.security.MessageDigest
import java.sql.{SQLType, PreparedStatement, Timestamp}
import java.text.SimpleDateFormat
import java.util.Date

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import scala.xml.{Elem, XML}

object GetData {

  def doDaysOfYear(year: Int, ds: HikariDataSource): Unit = {
    val from = s"${year}-01-01"
    val to = s"${year}-12-31"

    val hours = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23"

    //val element = "RR,SLAG"
    val element = "RR_1"
    // RR_1 = [Nedbør (1 time), mm] Nedbørmengde siste time
    // RR = [Nedbør, mm] Døgn- eller månedssum for nedbør (nedbørdøgn 07-07)
    // SLAG = [Nedbørslag, kode] Døgnverdi: Sammendrag av nedbørtyper i døgnet: 02 (alle typer snø), 03 (alle typer regn), 30 (kombinasjon av 02 og 03), 31 (dugg, rim, tåke)

    val tidsSerieTypeID = "2"
    // 0 = Døgnverdier
    // 2 = Viser observasjoner for periode og elementer fra valgte stasjoner == Timeverdier

    var station = "18815" // bygdøy
    // 18700 = blindern

    val url: String = s"http://eklima.met.no/metdata/MetDataService?invoke=getMetData&timeserietypeID=${tidsSerieTypeID}&format=&from=${from}&to=${to}&stations=${station}&elements=${element}&hours=${hours}&months=&username="

    val xml: Elem = XML.loadString(io.Source.fromFile(getUrlToFile(url), "UTF-8").mkString)

    val content = xml \\ "Envelope" \ "Body" \ "getMetDataResponse" \ "return" \ "timeStamp" \ "item"
    for (conn <- resource.managed(ds.getConnection)) {
      conn.setAutoCommit(false)
      var count = 0
      for (ps <- resource.managed(conn.prepareStatement("insert into precipitation (measure_time, rr, rr_quality, slag, slag_quality) values (?, ?, ?, ?, ?)"))) {
        content.foreach(child => {
          def sharedProp(attr: String) = { (child \\ attr).text}
          def prop(datatype: String)(attr: String) = { ((child \\ "item").filter(n => (n \ "id").text == datatype) \\ attr).text}
          val rrProp = prop("RR_1") _
          val slagProp = prop("SLAG") _

          val date: String = sharedProp("from")
          var value: String = if ("-1.0".equals(rrProp("value"))) "0.0" else rrProp("value")
          value = if ("-99999".equals(value)) "0.0" else value

          val quality: String = rrProp("quality")
          if ("7".equalsIgnoreCase(quality))
            return

          if (!("0,1,2,5,6".split(",").contains(quality)))
            throw new RuntimeException("Unknown quality: " + quality)

          val fullDate: Date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(date)
          val bd: BigDecimal = BigDecimal(value)
          ps.setTimestamp(1, new Timestamp(fullDate.getTime))
          ps.setBigDecimal(2, bd.bigDecimal)
          ps.setLong(3, quality.toLong)
          try {
            ps.setBigDecimal(4, BigDecimal(slagProp("value")).bigDecimal)
          } catch {
            case e : Exception => {
              println(s"date ${date} slag value was '" + slagProp("value") + "'")
              ps.setBigDecimal(4, BigDecimal("-1").bigDecimal)
            }
          }
          try {
            ps.setLong(5, slagProp("quality").toLong)
          } catch {
            case e : Exception => {
              println(s"date ${date} slag quality was ${slagProp("quality")}")
              ps.setLong(5, -1)
            }
          }
          ps.addBatch()
          count += 1
        })
        ps.executeBatch()
      }
      conn.commit()
      println("year " + year + " with " + count + " values")
    }
  }

  def getUrlToFile(url: String): File = {
    val cache: File = new File("../cache/" + MessageDigest.getInstance("MD5").digest(url.getBytes).map("%02X".format(_)).mkString)
    val cacheDir: File = new File("../cache/")
    if (!cacheDir.exists()) {
      cacheDir.mkdirs()
    }

    if (!cache.exists()) {
      println("downloading url " + url)
      val data = io.Source.fromURL(url, "UTF-8")
      val out = new PrintWriter(cache, "UTF-8")
      data.getLines.foreach(out.println(_))
      out.close()
      data.close()
      println("downloading url " + url + " done")
    }
    cache
  }

  def main(args: Array[String]) {
    val ds: HikariDataSource = getDataSource

    for (conn <- resource.managed(ds.getConnection);
         ps <- resource.managed(conn.prepareStatement("truncate table precipitation"))) {
      ps.execute()
      conn.commit()
    }
    (2012 to 2014) foreach(year => { doDaysOfYear(year, ds)})
    //doDaysOfYear(1980, ds)
  }

  def getDataSource: HikariDataSource = {
    val config: HikariConfig = new HikariConfig
    config.setMaximumPoolSize(2)
    config.setDriverClassName("oracle.jdbc.OracleDriver")
    config.setJdbcUrl("jdbc:oracle:thin:@//localhost:1521/pdb1")
    config.addDataSourceProperty("user", "ivref")
    config.addDataSourceProperty("password", Pw.pass())
    config.setConnectionTimeout(1500)
    val ds: HikariDataSource = new HikariDataSource(config)
    ds
  }
}
