import java.io.FileOutputStream
import java.sql.{PreparedStatement, ResultSetMetaData}

import scala.collection.immutable.Range.Inclusive
import scalax.io.{Codec, Resource}

object WriteDataTsv {
  def main(args: Array[String]) {
    val dataSource = GetData.getDataSource

    val connection = dataSource.getConnection

    val sql = io.Source.fromInputStream(getClass.getResourceAsStream("/30day_moving_average.sql"), "UTF-8").mkString
    println(sql)

    val ps: PreparedStatement = connection.prepareStatement(sql)
    val rs = ps.executeQuery()

    implicit val codec = Codec.UTF8

    for (out <- Resource.fromOutputStream(new FileOutputStream("src/main/resources/data.tsv")).outputProcessor) {
      val output = out.asOutput

      val metaData: ResultSetMetaData = rs.getMetaData

      println("-" * 80)
      val columns: Inclusive = 1 to metaData.getColumnCount

      columns.foreach(i => println(i + " = " + metaData.getColumnName(i) + " of type " + metaData.getColumnTypeName(i)))

      output.writeStrings(columns.map(metaData.getColumnName(_)), "\t")
      output.write("\n")

      while (rs.next()) {
        output.writeStrings(columns.map(rs.getString(_)), "\t")
        output.write("\n")
      }
    }

    rs.close()
    ps.close()
    connection.close()
  }
}
