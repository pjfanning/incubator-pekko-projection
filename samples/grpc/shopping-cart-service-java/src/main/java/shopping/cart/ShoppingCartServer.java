package shopping.cart;

//#startServer
import pekko.actor.typed.ActorSystem;
import pekko.grpc.javadsl.ServerReflection;
import pekko.grpc.javadsl.ServiceHandler;
import pekko.http.javadsl.Http;
import pekko.http.javadsl.ServerBinding;
import pekko.http.javadsl.model.HttpRequest;
import pekko.http.javadsl.model.HttpResponse;
import pekko.japi.function.Function;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import shopping.cart.proto.ShoppingCartService;
import shopping.cart.proto.ShoppingCartServiceHandlerFactory;

//#startServer

//#startServer
public final class ShoppingCartServer {

  private ShoppingCartServer() {}

  static void start(String host, int port, ActorSystem<?> system, ShoppingCartService grpcService, Function<HttpRequest, CompletionStage<HttpResponse>> eventProducerService) {
    @SuppressWarnings("unchecked")
    Function<HttpRequest, CompletionStage<HttpResponse>> service =
        ServiceHandler.concatOrNotFound(
            eventProducerService,
            ShoppingCartServiceHandlerFactory.create(grpcService, system),
            // ServerReflection enabled to support grpcurl without import-path and proto parameters
            ServerReflection.create(
                Collections.singletonList(ShoppingCartService.description), system));

    CompletionStage<ServerBinding> bound =
        Http.get(system).newServerAt(host, port).bind(service::apply);

    bound.whenComplete(
        (binding, ex) -> {
          if (binding != null) {
            binding.addToCoordinatedShutdown(Duration.ofSeconds(3), system);
            InetSocketAddress address = binding.localAddress();
            system
                .log()
                .info(
                    "Shopping online at gRPC server {}:{}",
                    address.getHostString(),
                    address.getPort());
          } else {
            system.log().error("Failed to bind gRPC endpoint, terminating system", ex);
            system.terminate();
          }
        });
  }
}
//#startServer
