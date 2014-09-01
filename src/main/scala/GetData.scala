import scala.xml.{Elem, XML}

object GetData {
  def main(args: Array[String]) {

    val from = "2006-01-01"
    val to = "2006-01-31"
    val element = "tam"

    // TAM = [Middeltemperatur, ºC] "Timeverdi: Aritmetisk middel av minuttverdier Døgnverdi: Aritmetisk middel av 24 timeverdier (kl 00-00), evt formelbasert middelverdi ut fra færre observasjoner (kl 18-18)Månedsverdi: Aritmetisk middel av døgn-TAM "

    val data = io.Source.fromURL(s"http://eklima.met.no/metdata/MetDataService?invoke=getMetData&timeserietypeID=0&format=&from=${from}&to=${to}&stations=18700&elements=${element}&hours=&months=&username=").mkString
    val xml: Elem = XML.loadString(data)

    //println(new PrettyPrinter(160, 1).format(xml))

    val content = xml \\ "Envelope" \ "Body" \ "getMetDataResponse" \ "return" \ "timeStamp" \ "item"
    content.foreach(child => {
      def prop(x: String) = { (child \\ x).text}
      println(s"${prop("from")} = quality: ${prop("quality")}, value: ${prop("value")}")
    })

  }
}
