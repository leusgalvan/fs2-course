package communication

import fs2._
import cats.effect._
import fs2.concurrent.SignallingRef
import scala.concurrent.duration._
import scala.util.Random

object Signals extends IOApp.Simple {
  override def run: IO[Unit] = {
    def signaller(signal: SignallingRef[IO, Boolean]): Stream[IO, Nothing] = {
      Stream
        .repeatEval(IO(Random.between(1, 1000)).flatTap(i => IO.println(s"Generating $i")))
        .metered(100.millis)
        .evalMap(i => if(i % 5 == 0) signal.set(true) else IO.unit)
        .drain
    }

    def worker(signal: SignallingRef[IO, Boolean]): Stream[IO, Nothing] = {
      Stream
        .repeatEval(IO.println("Working..."))
        .metered(50.millis)
        .interruptWhen(signal)
        .drain
    }

    Stream.eval(SignallingRef[IO, Boolean](false)).flatMap { signal =>
      worker(signal).concurrently(signaller(signal))
    }.compile.drain

    type Temperature = Double
    def createTemperatureSensor(alarm: SignallingRef[IO, Temperature], threshold: Temperature): Stream[IO, Nothing] =
      Stream
        .repeatEval(IO(Random.between(-40.0, 40.0)))
        .evalTap(t => IO.println(f"Current temperature: $t%.1f"))
        .evalMap(t => if(t > threshold) alarm.set(t) else IO.unit)
        .metered(300.millis)
        .drain

    def createCooler(alarm: SignallingRef[IO, Temperature]): Stream[IO, Nothing] =
      alarm
        .discrete
        .evalMap(t => IO.println(f"$t%.1f °C is too hot! Cooling down..."))
        .drain

    val threshold = 20.0
    val initialTemperature = 20.0

    // Exercise
    // Create a stream that emits a signal
    // Create a temperature sensor and a cooler
    // Run them concurrently
    // Interrupt after 3 seconds
    val program = Stream.eval(SignallingRef[IO, Temperature](initialTemperature)).flatMap { alarm =>
      val temperatureSensor = createTemperatureSensor(alarm, threshold)
      val cooler = createCooler(alarm)
      cooler.merge(temperatureSensor)
    }
    program.interruptAfter(3.seconds).compile.drain
  }
}