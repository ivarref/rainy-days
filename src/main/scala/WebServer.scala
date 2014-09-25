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
              val sql = "select measure_time, rain from rain where to_char(measure_time, 'yyyy') = '2014' order by measure_time"
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
        addHandler(new ResourceHandler {
          setBaseResource(new FileResource(new File("src/main/resources").toURI))
        }
        )
      })
    }
    server.start()
    server.join()
  }

}
