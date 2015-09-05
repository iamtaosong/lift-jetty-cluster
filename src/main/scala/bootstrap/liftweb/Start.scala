package bootstrap.liftweb

import net.liftweb.common.Loggable
import net.liftweb.util.{StringHelpers, LoggingAutoConfigurer, Props}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.{JDBCSessionManager, JDBCSessionIdManager}
import org.eclipse.jetty.webapp.WebAppContext
import util.Properties

object Start extends App with Loggable {

  LoggingAutoConfigurer().apply()

  logger.info("run.mode: " + Props.modeName)
  logger.trace("system environment: " + sys.env)
  logger.trace("system props: " + sys.props)
  logger.info("liftweb props: " + Props.props)
  logger.info("args: " + args.toList)

  startLift()
  
  def startLift(): Unit = {
    logger.info("starting Lift server")

    val port = {
      val prop = Props.get("jetty.port", "8080")
      val str = if(prop startsWith "$") System.getenv(prop substring 1) else prop
      str.toInt
    }

    logger.info(s"port number is $port")

    val webappDir: String = Option(this.getClass.getClassLoader.getResource("webapp"))
      .map(_.toExternalForm)
      .filter(_.contains("jar:file:")) // this is a hack to distinguish in-jar mode from "expanded"
      .getOrElse("target/webapp")

    logger.info(s"webappDir: $webappDir")

    val server = new Server(port)
    val context = new WebAppContext(webappDir, Props.get("jetty.contextPath").openOr("/"))

    if(Props.get("cluster").map(_.equalsIgnoreCase("true")).openOr(false)) {
      val workerName = StringHelpers.randomString(10)

      logger.info(s"WorkerName: $workerName")

      val dbHost = Properties.envOrElse("DB_HOST", "127.0.0.1")
      val dbPort = Properties.envOrElse("DB_PORT", "3306")
      val driver = Props.get("cluster.jdbc.driver").openOrThrowException("Cannot boot in cluster mode without property 'session.jdbc.driver' defined in props file")
      val endpoint = s"jdbc:mysql://$dbHost:$dbPort/lift_sessions?user=jetty&password=lift-rocks"
      val idMgr = new JDBCSessionIdManager(server)
      idMgr.setWorkerName(workerName)
      idMgr.setDriverInfo(driver, endpoint)
      idMgr.setScavengeInterval(60)
      server.setSessionIdManager(idMgr)

      val jdbcMgr = new JDBCSessionManager()
      jdbcMgr.setSessionIdManager(server.getSessionIdManager())
      context.getSessionHandler().setSessionManager(jdbcMgr)
    }

    server.setHandler(context)
    server.start()
    logger.info(s"Lift server started on port $port")
    server.join()
  }

}
