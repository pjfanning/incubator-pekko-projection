package shopping.analytics

import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.ActorSystem
import pekko.management.cluster.bootstrap.ClusterBootstrap
import pekko.management.scaladsl.PekkoManagement
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal

object Main {

  val logger = LoggerFactory.getLogger("shopping.analytics.Main")

  def main(args: Array[String]): Unit = {
    val system =
      ActorSystem[Nothing](Behaviors.empty, "ShoppingAnalyticsService")
    try {
      init(system)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization failure.", e)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_]): Unit = {
    PekkoManagement(system).start()
    ClusterBootstrap(system).start()

    ShoppingCartEventConsumer.init(system)
  }

}
