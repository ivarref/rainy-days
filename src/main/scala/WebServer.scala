import java.io.File
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.server.handler.{HandlerList, ResourceHandler}
import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.eclipse.jetty.util.resource.FileResource

import resource._

object WebServer {

  def main(args: Array[String]) {
    val ds = GetData.getDataSource
    val server: Server = new Server() {
      setConnectors(Array(new ServerConnector(this) {
        setPort(8080)
      }))

      setHandler(new HandlerList {
        addHandler(new ServletContextHandler() {
          setContextPath("/data")
          addServlet(new ServletHolder(new HttpServlet {
            override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
              val year = req.getParameterMap.getOrDefault("year", Array("2013"))(0)
              val sql = "select measure_time, rain from rain where to_char(measure_time, 'yyyy') = '" + Integer.valueOf(year) + "' order by measure_time"
              val writer = resp.getWriter
              writer.println("date\train")
              for (conn <- managed(ds.getConnection);
                   ps <- managed(conn.prepareStatement(sql));
                   rs <- managed(ps.executeQuery())) {
                Stream.continually(rs.next()).takeWhile(_ == true).foreach(_ => {
                  writer.print(rs.getDate(1))
                  writer.print('\t')
                  writer.print(rs.getBigDecimal(2))
                  writer.print('\n')
                })
              }
            }
          }), "/*")
        })
        addHandler(new ServletContextHandler() {
          setContextPath("/distribution")
          addServlet(new ServletHolder(new HttpServlet {
            override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
              val year = req.getParameterMap.getOrDefault("year", Array("2013"))(0)
              val delta = Integer.valueOf(req.getParameterMap.getOrDefault("delta", Array("15"))(0))
              val stddev = BigDecimal("5.84143481732568463343272336842832695822") // stddev for 1950 - 1980 where rain > 0
              val sql = "" +
                "with t1 as " +
                "( " +
                "  select number_of_stddevs, count(*) as antall from" +
                "   (" +
                "    select rain / " +
                  "   (select stddev(rain) from rain where rain>0 and to_char(measure_time, 'yyyy') between '1950' and '1980') " +
                  "  as number_of_stddevs from rain " +
                "    where rain>0 " +
                "      and to_char(measure_time, 'yyyy') between ? and ? " +
                "   ) where number_of_stddevs <=8 group by number_of_stddevs order by number_of_stddevs asc" +
                ") " +
                "select number_of_stddevs, antall, round(" +
                  "(100.0*(select sum(antall) from t1 b where b.number_of_stddevs >= a.number_of_stddevs))" +
                  "/ (select sum(antall) from t1), 1) from t1 a"
              val writer = resp.getWriter
              for (conn <- managed(ds.getConnection);
                   ps <- managed(conn.prepareStatement(sql))) {
                //ps.setBigDecimal(1, stddev.bigDecimal)
                //ps.setBigDecimal(2, stddev.bigDecimal)
                ps.setString(1, String.valueOf(Integer.valueOf(year)-delta))
                ps.setString(2, String.valueOf(Integer.valueOf(year)))
                writer.println("number_of_stddevs\tcount\tpercentage")
                for (rs <- managed(ps.executeQuery())) {
                  Stream.continually(rs.next()).takeWhile(_ == true).foreach(_ => {
                    writer.print(rs.getBigDecimal(1)); writer.print('\t')
                    writer.print(rs.getBigDecimal(2)); writer.print('\t')
                    writer.print(rs.getBigDecimal(3)); writer.print('\t')
                    writer.print('\n')
                  })
                }
              }
            }
          }), "/*")
        })
        addHandler(new ResourceHandler {
          setBaseResource(new FileResource(new File("src/main/resources").toURI))
        })
      })
    }

    server.start()
    server.join()
  }

}
