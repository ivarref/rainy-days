import java.sql.{PreparedStatement, Timestamp}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import scala.xml.{Elem, XML}

object GetData {
  def main(args: Array[String]) {
    val config: HikariConfig = new HikariConfig
    config.setMaximumPoolSize(2)
    config.setDriverClassName("oracle.jdbc.OracleDriver")
    config.setJdbcUrl("jdbc:oracle:thin:@//localhost:1521/pdb1")
    config.addDataSourceProperty("user", "ivref")
    config.addDataSourceProperty("password", Pw.pass())
    config.setConnectionTimeout(1500)
    val ds: HikariDataSource = new HikariDataSource(config)

    /*
    val from = "2006-01-01"
    val to = "2006-01-01"
    */
    val from = "2014-01-01"
    val to = "2014-09-08"
    val element = "RR_1"
    val hours = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23"
    val url: String = s"http://eklima.met.no/metdata/MetDataService?invoke=getMetData&timeserietypeID=2&format=&from=${from}&to=${to}&stations=18700&elements=${element}&hours=${hours}&months=&username="
    val data = io.Source.fromURL(url).mkString
    val xml: Elem = XML.loadString(data)
    println("url is ")
    println(url)
    //println(new PrettyPrinter(160, 1).format(xml))

    val content = xml \\ "Envelope" \ "Body" \ "getMetDataResponse" \ "return" \ "timeStamp" \ "item"
    val conn = ds.getConnection
    conn.setAutoCommit(false)

    conn.prepareStatement("truncate table rain").execute()

    content.foreach(child => {
      def prop(x: String) = { (child \\ x).text}
      val date: String = prop("from")
      var value: String = if ("-1.0".equals(prop("value"))) "0.0" else prop("value")
      value = if ("-99999".equals(value)) "0.0" else value

      val quality: String = prop("quality")
      println(s"${date} = quality: ${quality}, value: ${value}")
      if (!("0,1,2,5".split(",").contains(quality)))
        throw new RuntimeException("Unknown quality: " + quality)

      val fullDate: Date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(date)
      val ps: PreparedStatement = conn.prepareStatement("insert into rain (measure_time, rain, quality) values (?, ?, ?)")
      val bd: BigDecimal = BigDecimal(value)
      ps.setTimestamp(1, new Timestamp(fullDate.getTime))
      ps.setBigDecimal(2, bd.bigDecimal)
      ps.setLong(3, quality.toLong)
      ps.executeUpdate()
      ps.close()
    })

    conn.commit()
    conn.close()
  }
}
