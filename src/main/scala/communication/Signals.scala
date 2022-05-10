package communication

import cats.effect._
import fs2._
import fs2.concurrent._

import scala.concurrent.duration._
import scala.util.Random

object Signals extends IOApp.Simple {
  override def run: IO[Unit] = {
    // Boolean signal
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

    SignallingRef[IO, Int](0).flatMap { signal =>
      val p = Stream.iterate(1)(_ + 1).covary[IO].metered(100.millis).evalMap(signal.set).drain
      val c = signal.discrete.evalMap(i => IO.println(s"Read $i")).drain
      c.concurrently(p).interruptAfter(3.seconds).compile.drain
    }
    
    // Double signal
    type Temperature = Double

    // Exercise
    def createTemperatureSensor(alarm: SignallingRef[IO, Temperature], threshold: Temperature): Stream[IO, Nothing] = {
      Stream
        .repeatEval(IO(Random.between(-40.0, 40.0)))
        .evalTap(t => IO.println(f"Current temperature: $t%.1f"))
        .evalMap(t => if(t > threshold) alarm.set(t) else IO.unit)
        .metered(300.millis)
        .drain
    }

    def createCooler(alarm: SignallingRef[IO, Temperature]) = {
      alarm
        .discrete
        .evalMap(t => IO.println(f"$t%.1f °C is too hot! Cooling down..."))
        .drain
    }

    val threshold = 20
    val initialTemperature = threshold

    val program = Stream.eval(SignallingRef[IO, Temperature](initialTemperature)).flatMap { signal =>
      val temperatureSensor = createTemperatureSensor(signal, threshold)
      val cooler = createCooler(signal)
      cooler.mergeHaltBoth(temperatureSensor)
    }
    program.interruptAfter(3.seconds).compile.drain
  }
}
