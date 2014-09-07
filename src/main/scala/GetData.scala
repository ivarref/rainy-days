import java.sql.{PreparedStatement, Timestamp}
import java.text.SimpleDateFormat
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

    val from = "2006-01-01"
    val to = "2006-12-31"
    val element = "RR"

    val data = io.Source.fromURL(s"http://eklima.met.no/metdata/MetDataService?invoke=getMetData&timeserietypeID=0&format=&from=${from}&to=${to}&stations=18700&elements=${element}&hours=&months=&username=").mkString
    val xml: Elem = XML.loadString(data)

    //println(new PrettyPrinter(160, 1).format(xml))

    val content = xml \\ "Envelope" \ "Body" \ "getMetDataResponse" \ "return" \ "timeStamp" \ "item"
    val conn = ds.getConnection
    conn.setAutoCommit(false)

    conn.prepareStatement("truncate table rain").execute()

    content.foreach(child => {
      def prop(x: String) = { (child \\ x).text}
      val date: String = prop("from")
      val value: String = if ("-1.0".equals(prop("value"))) "0.0" else prop("value")
      val quality: String = prop("quality")
      println(s"${date} = quality: ${quality}, value: ${value}")
      if (!"2".equals(quality))
        throw new RuntimeException("Bad quality: " + quality)

      val ps: PreparedStatement = conn.prepareStatement("insert into rain (measure_time, rain) values (?, ?)")
      val dateParsed: Date = new SimpleDateFormat("yyyy-MM-dd").parse(date)
      ps.setTimestamp(1, new Timestamp(dateParsed.getTime))
      val bd: BigDecimal = BigDecimal(value)
      ps.setBigDecimal(2, bd.bigDecimal)
      ps.executeUpdate()
      ps.close()
    })

    conn.commit()
    conn.close()

  }
}
