import java.io.File
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.server.handler.{HandlerList, ResourceHandler}
import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.eclipse.jetty.util.resource.FileResource

object WebServer {

  def main(args: Array[String]) {

    val server: Server = new Server()
    val connector: ServerConnector  = new ServerConnector(server)
    connector.setPort(8080)
    server.setConnectors(Array(connector))

    val handlers: HandlerList = new HandlerList

    val resourceHandler: ResourceHandler = new ResourceHandler
    resourceHandler.setBaseResource(new FileResource(new File("src/main/resources").toURI))

    val servletHandler = new ServletContextHandler()
    servletHandler.setContextPath("/data")

    val servlet = new HttpServlet {
      override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
        resp.getWriter.append("Hello world.")
      }
    }
    servletHandler.addServlet(new ServletHolder(servlet), "/*")

    handlers.addHandler(servletHandler)
    handlers.addHandler(resourceHandler)

    server.setHandler(handlers)
    server.start()
    server.join()
  }

}
