import scala.xml.{Elem, XML}

object ListSeriesTypes {

  def main(args: Array[String]) {

    val timeSeriesTypes = io.Source.fromURL("http://eklima.met.no/metdata/MetDataService?invoke=getTimeserieTypesProperties&language=&timeserieTypes=").mkString
    val xml: Elem = XML.loadString(timeSeriesTypes)

    //println(new PrettyPrinter(80, 2).format(xml))

    val content = xml \\ "Envelope" \ "Body" \ "getTimeserieTypesPropertiesResponse" \ "return"
    val children = content \ "item"
    children.foreach(x => {
      println(s"${(x \ "serieTypeID").text} = ${(x \ "serieTypeDescription").text}")
    })
  }
}