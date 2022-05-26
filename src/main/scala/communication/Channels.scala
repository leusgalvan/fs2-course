package communication

import fs2._
import fs2.concurrent._
import cats.effect._

import scala.concurrent.duration._
import cats._
import cats.implicits._

import scala.util.Random

object Channels extends IOApp.Simple {
  override def run: IO[Unit] = {
    Stream.eval(Channel.bounded[IO, Int](1)).flatMap { channel =>
      val p = Stream.iterate(1)(_ + 1).covary[IO].evalMap(channel.send).drain
      val c = channel.stream.metered(200.millis).evalMap(i => IO.println(s"Read $i")).drain
      c.concurrently(p).interruptAfter(3.seconds)
    }.compile.drain

    sealed trait Measurement
    case class Temperature(value: Double) extends Measurement
    case class Humidity(value: Double) extends Measurement

    implicit val ordHum: Order[Humidity] = Order.by(_.value)
    implicit val ordTem: Order[Temperature] = Order.by(_.value)

    def createTemperatureSensor(alarm: Channel[IO, Measurement], threshold: Temperature): Stream[IO, Nothing] = {
      Stream
        .repeatEval(IO(Temperature(Random.between(-40.0, 40.0))))
        .evalTap(t => IO.println(f"Current temperature: ${t.value}%.1f"))
        .evalMap(t => if(t > threshold) alarm.send(t) else IO.unit)
        .metered(300.millis)
        .drain
    }

    // Exercise
    // Repeatedly generate random humidities between 0.0 and 100.0
    // Print every humidity as the current humidity
    // Check if the humidity goes above the threshold and send an alarm
    // Assume that we read each humidity every 100 milliseconds
    def createHumiditySensor(alarm: Channel[IO, Measurement], threshold: Humidity): Stream[IO, Nothing] =
      Stream
        .repeatEval(IO(Humidity(Random.between(0.0, 100.0))))
        .evalTap(h => IO.println(f"Current humidity: ${h.value}%.1f"))
        .evalMap(h => if(h > threshold) alarm.send(h) else IO.unit)
        .metered(100.millis)
        .drain

    // Read the values from the channel
    // Handle alarms by outputting something to console
    def createCooler(alarm: Channel[IO, Measurement]): Stream[IO, Nothing] =
      alarm
        .stream
        .evalMap {
          case Temperature(t) => IO.println(f"$t%.1f °C is too hot! Cooling down...")
          case Humidity(h) => IO.println(f"$h%.1f %% is too humid! Drying...")
        }
        .drain

    val temperatureThreshold = Temperature(10.0)
    val humidityThreshold = Humidity(50.0)

    // Exercise
    // Create a stream that emits a new unbounded channel
    // Create one of each sensor and a cooler
    // Run all streams concurrently
    // Interrupt after 3 seconds
    val program = Stream.eval(Channel.unbounded[IO, Measurement]).flatMap { alarmChannel =>
      val temperatureSensor = createTemperatureSensor(alarmChannel, temperatureThreshold)
      val humiditySensor = createHumiditySensor(alarmChannel, humidityThreshold)
      val cooler = createCooler(alarmChannel)
      Stream(temperatureSensor, humiditySensor, cooler).parJoinUnbounded
    }
    program.interruptAfter(3.seconds).compile.drain
  }
}