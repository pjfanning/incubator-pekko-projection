package shopping.cart;

import pekko.actor.typed.ActorSystem;
import pekko.actor.typed.javadsl.Behaviors;
import pekko.http.javadsl.model.HttpRequest;
import pekko.http.javadsl.model.HttpResponse;
import pekko.japi.function.Function;
import pekko.management.cluster.bootstrap.ClusterBootstrap;
import pekko.management.javadsl.PekkoManagement;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.proto.ShoppingCartService;

import java.util.concurrent.CompletionStage;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "ShoppingCartService");
    try {
      init(system);
    } catch (Exception e) {
      logger.error("Terminating due to initialization failure.", e);
      system.terminate();
    }
  }

  public static void init(ActorSystem<Void> system) {
    PekkoManagement.get(system).start();
    ClusterBootstrap.get(system).start();

    ShoppingCart.init(system);

    Function<HttpRequest, CompletionStage<HttpResponse>> eventProducerService = PublishEvents.eventProducerService(system);

    Config config = system.settings().config();
    String grpcInterface = config.getString("shopping-cart-service.grpc.interface");
    int grpcPort = config.getInt("shopping-cart-service.grpc.port");
    ShoppingCartService grpcService = new ShoppingCartServiceImpl(system);
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService, eventProducerService);
  }

}
