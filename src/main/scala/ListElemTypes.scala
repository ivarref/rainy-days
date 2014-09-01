import scala.xml.{XML, Elem}

object ListElemTypes {

  def main(args: Array[String]) {
    val elemTypes = io.Source.fromURL("http://eklima.met.no/metdata/MetDataService?invoke=getElementsProperties&language=&elem_codes=").mkString
    val xml: Elem = XML.loadString(elemTypes)

    val content = xml \\ "Envelope" \ "Body" \ "getElementsPropertiesResponse" \ "return" \ "item"
    content.foreach(child => {
      def prop(x: String) = { (child \ x).text}
      println(s"${prop("elemCode")} = [${prop("name")}, ${prop("unit")}] ${prop("description")}")
    })

  }
}
