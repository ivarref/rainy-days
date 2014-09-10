import java.io.{File, PrintWriter}
import java.security.MessageDigest
import java.sql.{PreparedStatement, Timestamp}
import java.text.SimpleDateFormat
import java.util.Date

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import scala.xml.{Elem, XML}

object GetData {

  def doHoursOfYear(year: Int, ds: HikariDataSource): Unit = {
    val from = s"${year}-01-01"
    val to = s"${year}-12-31"

    val hours = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23"


    val element = "RR"
    // RR_1 = [Nedbør (1 time), mm] Nedbørmengde siste time
    // RR = [Nedbør, mm] Døgn- eller månedssum for nedbør (nedbørdøgn 07-07)

    val tidsSerieTypeID = "0"
    // 0 = Døgnverdier
    // 2 = Viser observasjoner for periode og elementer fra valgte stasjoner == Timeverdier

    val url: String = s"http://eklima.met.no/metdata/MetDataService?invoke=getMetData&timeserietypeID=${tidsSerieTypeID}&format=&from=${from}&to=${to}&stations=18700&elements=${element}&hours=${hours}&months=&username="

    val xml: Elem = XML.loadString(io.Source.fromFile(getUrlToFile(url), "UTF-8").mkString)

    val content = xml \\ "Envelope" \ "Body" \ "getMetDataResponse" \ "return" \ "timeStamp" \ "item"
    val conn = ds.getConnection
    conn.setAutoCommit(false)

    var count = 0
    val ps: PreparedStatement = conn.prepareStatement("insert into rain (measure_time, rain, quality) values (?, ?, ?)")
    content.foreach(child => {
      def prop(x: String) = { (child \\ x).text}
      val date: String = prop("from")
      var value: String = if ("-1.0".equals(prop("value"))) "0.0" else prop("value")
      value = if ("-99999".equals(value)) "0.0" else value

      val quality: String = prop("quality")
      //println(s"${date} = quality: ${quality}, value: ${value}")
      if (!("0,1,2,5,6".split(",").contains(quality)))
        throw new RuntimeException("Unknown quality: " + quality)

      val fullDate: Date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(date)
      val bd: BigDecimal = BigDecimal(value)
      ps.setTimestamp(1, new Timestamp(fullDate.getTime))
      ps.setBigDecimal(2, bd.bigDecimal)
      ps.setLong(3, quality.toLong)
      ps.addBatch()
      count += 1
    })

    ps.executeBatch()
    ps.close()

    println("year " + year + " with " + count + " values")

    conn.commit()
    conn.close()
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
    val config: HikariConfig = new HikariConfig
    config.setMaximumPoolSize(2)
    config.setDriverClassName("oracle.jdbc.OracleDriver")
    config.setJdbcUrl("jdbc:oracle:thin:@//localhost:1521/pdb1")
    config.addDataSourceProperty("user", "ivref")
    config.addDataSourceProperty("password", Pw.pass())
    config.setConnectionTimeout(1500)
    val ds: HikariDataSource = new HikariDataSource(config)

    val conn = ds.getConnection
    conn.prepareStatement("truncate table rain").execute()
    conn.commit()
    conn.close()

    (1900 to 2014) foreach(year => {
      doHoursOfYear(year, ds)
    })

  }
}
