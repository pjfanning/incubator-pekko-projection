package shopping.analytics

//#initProjections
import scala.concurrent.Future

import pekko.Done
import pekko.actor.typed.ActorSystem
import pekko.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import pekko.persistence.Persistence
import pekko.persistence.query.typed.EventEnvelope
import pekko.projection.ProjectionBehavior
import pekko.projection.ProjectionId
import pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import pekko.projection.grpc.consumer.scaladsl.GrpcReadJournal
import pekko.projection.r2dbc.scaladsl.R2dbcProjection
import pekko.projection.scaladsl.Handler
import org.slf4j.LoggerFactory
import shoppingcart.CheckedOut
import shoppingcart.ItemAdded
import shoppingcart.ItemQuantityAdjusted
import shoppingcart.ItemRemoved
import shoppingcart.ShoppingCartEventsProto

object ShoppingCartEventConsumer {
  //#initProjections

  private val log =
    LoggerFactory.getLogger("shopping.analytics.ShoppingCartEventConsumer")

  //#eventHandler
  private class EventHandler(projectionId: ProjectionId)
      extends Handler[EventEnvelope[AnyRef]] {
    private var totalCount = 0
    private var throughputStartTime = System.nanoTime()
    private var throughputCount = 0

    override def start(): Future[Done] = {
      log.info("Started Projection [{}].", projectionId.id)
      super.start()
    }
    override def stop(): Future[Done] = {
      log.info(
        "Stopped Projection [{}]. Consumed [{}] events.",
        projectionId.id,
        totalCount)
      super.stop()
    }

    override def process(envelope: EventEnvelope[AnyRef]): Future[Done] = {
      val event = envelope.event
      totalCount += 1

      event match {
        case itemAdded: ItemAdded =>
          log.info(
            "Projection [{}] consumed ItemAdded for cart {}, added {} {}. Total [{}] events.",
            projectionId.id,
            itemAdded.cartId,
            itemAdded.quantity,
            itemAdded.itemId,
            totalCount)
        case quantityAdjusted: ItemQuantityAdjusted =>
          log.info(
            "Projection [{}] consumed ItemQuantityAdjusted for cart {}, changed {} {}. Total [{}] events.",
            projectionId.id,
            quantityAdjusted.cartId,
            quantityAdjusted.quantity,
            quantityAdjusted.itemId,
            totalCount)
        case itemRemoved: ItemRemoved =>
          log.info(
            "Projection [{}] consumed ItemRemoved for cart {}, removed {}. Total [{}] events.",
            projectionId.id,
            itemRemoved.cartId,
            itemRemoved.itemId,
            totalCount)
        case checkedOut: CheckedOut =>
          log.info(
            "Projection [{}] consumed CheckedOut for cart {}. Total [{}] events.",
            projectionId.id,
            checkedOut.cartId,
            totalCount)
        case unknown =>
          throw new IllegalArgumentException(s"Unknown event $unknown")
      }

      throughputCount += 1
      val durationMs: Long =
        (System.nanoTime - throughputStartTime) / 1000 / 1000
      if (throughputCount >= 1000 || durationMs >= 10000) {
        log.info(
          "Projection [{}] throughput [{}] events/s in [{}] ms",
          projectionId.id,
          1000L * throughputCount / durationMs,
          durationMs)
        throughputCount = 0
        throughputStartTime = System.nanoTime
      }
      Future.successful(Done)
    }
  }
  //#eventHandler

  //#initProjections
  def init(system: ActorSystem[_]): Unit = {
    implicit val sys: ActorSystem[_] = system
    val numberOfProjectionInstances = 4
    val projectionName: String = "cart-events"
    val sliceRanges =
      Persistence(system).sliceRanges(numberOfProjectionInstances)

    val eventsBySlicesQuery =
      GrpcReadJournal(List(ShoppingCartEventsProto.javaDescriptor))

    ShardedDaemonProcess(system).init(
      projectionName,
      numberOfProjectionInstances,
      { idx =>
        val sliceRange = sliceRanges(idx)
        val projectionKey =
          s"${eventsBySlicesQuery.streamId}-${sliceRange.min}-${sliceRange.max}"
        val projectionId = ProjectionId.of(projectionName, projectionKey)

        val sourceProvider = EventSourcedProvider.eventsBySlices[AnyRef](
          system,
          eventsBySlicesQuery,
          eventsBySlicesQuery.streamId,
          sliceRange.min,
          sliceRange.max)

        ProjectionBehavior(
          R2dbcProjection.atLeastOnceAsync(
            projectionId,
            None,
            sourceProvider,
            () => new EventHandler(projectionId)))
      })
  }

}
//#initProjections
