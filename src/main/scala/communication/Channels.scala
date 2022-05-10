package communication

import fs2._
import fs2.concurrent._
import cats.effect._
import cats._
import cats.implicits._

import scala.util.Random
import scala.concurrent.duration._

object Channels extends IOApp.Simple {
  override def run: IO[Unit] = {
    Channel.unbounded[IO, Int].flatMap { channel =>
      val p = Stream.iterate(1)(_ + 1).covary[IO].evalMap(channel.send).drain
      val c = channel.stream.evalMap(i => IO.println(s"Read $i")).drain
      c.concurrently(p).interruptAfter(3.seconds).compile.drain
    }

    sealed trait Measurement
    case class Temperature(value: Double) extends Measurement
    case class Humidity(value: Double) extends Measurement

    implicit val ordHum: Order[Humidity] = Order.by(_.value)
    implicit val ordTemp: Order[Temperature] = Order.by(_.value)

    // Exercise
    def createHumiditySensor(alarm: Channel[IO, Measurement], threshold: Humidity): Stream[IO, Nothing] = {
      Stream
        .repeatEval(IO(Humidity(Random.between(0.0, 100.0))))
        .evalTap(t => IO.println(f"Current humidity: ${t.value}%.1f"))
        .evalMap(t => if(t > threshold) alarm.send(t) else IO.unit)
        .metered(100.millis)
        .drain
    }

    def createTemperatureSensor(alarm: Channel[IO, Measurement], threshold: Temperature): Stream[IO, Nothing] = {
      Stream
        .repeatEval(IO(Temperature(Random.between(-40.0, 40.0))))
        .evalTap(t => IO.println(f"Current temperature: ${t.value}%.1f"))
        .evalMap(t => if(t > threshold) alarm.send(t) else IO.unit)
        .metered(300.millis)
        .drain
    }

    def createCooler(alarm: Channel[IO, Measurement]): Stream[IO, Nothing] = {
      alarm
        .stream
        .evalMap {
          case Temperature(t) =>
            IO.println(f"$t%.1f °C is too hot! Cooling down...")
          case Humidity(h) =>
            IO.println(f"$h%.1f %% is too humid! Drying...")
        }
        .drain
    }

    val temperatureThreshold = Temperature(10.0)
    val humidityThreshold = Humidity(70.0)

    val program = Stream.eval(Channel.unbounded[IO, Measurement]).flatMap { alarmChannel =>
      val temperatureSensor = createTemperatureSensor(alarmChannel, temperatureThreshold)
      val humiditySensor = createHumiditySensor(alarmChannel, humidityThreshold)
      val cooler = createCooler(alarmChannel)
      Stream(temperatureSensor, humiditySensor, cooler).parJoinUnbounded
    }

    program.interruptAfter(3.seconds).compile.drain
  }
}
