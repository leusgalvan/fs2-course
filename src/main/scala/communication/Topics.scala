package communication

import cats.effect._
import fs2._
import fs2.concurrent._

import scala.concurrent.duration._
import scala.util.Random

object Topics extends IOApp.Simple {
  override def run: IO[Unit] = {
    Topic[IO, Int].flatMap { topic =>
      val oneP = Stream.range(1, 10).through(topic.publish).drain
      //val slowP = Stream.iterate(1)(_ + 1).covary[IO].metered(100.millis).through(topic.publish).drain
      val fastP = Stream.iterate(1)(_ + 1).covary[IO].through(topic.publish).drain
      val slowC = topic.subscribe(0).evalMap(i => IO.sleep(300.millis) *> IO.println(s"Slowly read $i")).drain
      val fastC = topic.subscribe(10).evalMap(i => IO.println(s"Quickly read $i")).drain
      Stream(fastC, slowC, oneP).parJoinUnbounded.interruptAfter(2.seconds).compile.drain
    }

    case class CarPosition(carId: Long, lat: Double, lng: Double)

    def createCar(carId: Long, topic: Topic[IO, CarPosition]): Stream[IO, Nothing] = {
      Stream
        .repeatEval(IO(CarPosition(carId, Random.between(-90.0, 90.0), Random.between(-180.0, 180.0))))
        .metered(1000.millis)
        .through(topic.publish)
        .drain
    }

    def createGoogleMapUpdater(topic: Topic[IO, CarPosition]): Stream[IO, Nothing] = {
      topic
        .subscribe(10)
        .evalMap(pos => IO.println(f"Drawing position (${pos.lat}%.2f, ${pos.lng}%.2f) for car ${pos.carId} in map..."))
        .drain
    }

    // Exercise
    def createDriverNotifier(
      topic: Topic[IO, CarPosition],
      shouldNotify: CarPosition => Boolean,
      notify: CarPosition => IO[Unit]
    ): Stream[IO, Nothing] = {
      topic
        .subscribe(10)
        .evalMap(pos => if(shouldNotify(pos)) notify(pos) else IO.unit)
        .drain
    }

    Topic[IO, CarPosition].flatMap { topic =>
      val cars = Stream.range(1, 10).map(carId => createCar(carId, topic))
      val googleMapUpdater = createGoogleMapUpdater(topic)
      val driverNotifier = createDriverNotifier(
        topic = topic,
        shouldNotify = pos => pos.lat > 0.0,
        notify = pos => IO.println(f"Car ${pos.carId}: you are above the equator! (${pos.lat}%.2f, ${pos.lng}%.2f)")
      )
      (cars ++ Stream(googleMapUpdater, driverNotifier))
        .parJoinUnbounded
        .interruptAfter(3.seconds)
        .compile
        .drain
    }
  }
}
