package shopping.cart

import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.ActorSystem
import pekko.management.cluster.bootstrap.ClusterBootstrap
import pekko.management.scaladsl.PekkoManagement
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal

object Main {

  val logger = LoggerFactory.getLogger("shopping.cart.Main")

  def main(args: Array[String]): Unit = {
    val system =
      ActorSystem[Nothing](Behaviors.empty, "ShoppingCartService")
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

    ShoppingCart.init(system)

    val eventProducerService = PublishEvents.eventProducerService(system)

    val grpcInterface =
      system.settings.config.getString("shopping-cart-service.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("shopping-cart-service.grpc.port")
    val grpcService = new ShoppingCartServiceImpl(system)
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService, eventProducerService)
  }

}
