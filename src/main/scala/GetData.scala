import java.io.{FileWriter, BufferedWriter, File, PrintWriter}
import java.nio.charset.Charset

import scala.xml.{Elem, XML}

object GetData {
  def main(args: Array[String]) {

    val from = "2006-01-01"
    val to = "2006-12-31"
    val element = "RR"

    val data = io.Source.fromURL(s"http://eklima.met.no/metdata/MetDataService?invoke=getMetData&timeserietypeID=0&format=&from=${from}&to=${to}&stations=18700&elements=${element}&hours=&months=&username=").mkString
    val xml: Elem = XML.loadString(data)

    //println(new PrettyPrinter(160, 1).format(xml))

    val content = xml \\ "Envelope" \ "Body" \ "getMetDataResponse" \ "return" \ "timeStamp" \ "item"
    val output = new PrintWriter("src/main/resources/data.tsv", "UTF-8")

    output.write("date\train\n")
    content.foreach(child => {
      def prop(x: String) = { (child \\ x).text}
      val date: String = prop("from")
      val value: String = if ("-1.0".equals(prop("value"))) "0.0" else prop("value")
      val quality: String = prop("quality")
      println(s"${date} = quality: ${quality}, value: ${value}")
      if (!"2".equals(quality))
        throw new RuntimeException("Bad quality: " + quality)

      output.write(date.split('T')(0) + "\t" + value)
      output.write("\n")
    })
    output.close()

  }
}
