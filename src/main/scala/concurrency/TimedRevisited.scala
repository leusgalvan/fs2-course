package concurrency

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import fs2._
import cats.effect._

import scala.concurrent.duration._
import scala.util.Random

object TimedRevisited extends IOApp.Simple {
  override def run: IO[Unit] = {
    // Fixed delay
    // Fixed rate
    // Awake delay
    // Awake every
    val format = DateTimeFormatter.ofPattern("hh:MM:ss")
    def printNow: IO[Unit] = IO.println(LocalDateTime.now().format(format))
    def randomResult: IO[Int] = IO(Random.between(1, 1000))

    def process(executionTime: FiniteDuration = 0.seconds): IO[Int] =
      IO.sleep(executionTime) *> randomResult.flatTap(_ => printNow)

    val fixedRateStream = Stream.fixedRate[IO](2.seconds)
    fixedRateStream.take(3).compile.toList.flatMap(IO.println)

    val s = Stream.repeatEval(process(1.second))
    s.take(3).compile.toList.flatMap(IO.println)

    fixedRateStream.zip(s).take(3).compile.toList.flatMap(IO.println) // pull left -> pull right -> pull left -> ...

    // Exercise #1
    def metered[A](s: Stream[IO, A], d: FiniteDuration): Stream[IO, A] =
      Stream.fixedRate[IO](d).zipRight(s)

    metered(s, 1.second).take(3).compile.toList.flatMap(IO.println)

    val fixedDelayStream = Stream.fixedDelay[IO](2.seconds)
    fixedDelayStream.take(3).compile.toList.flatMap(IO.println)

    fixedDelayStream.zip(s).take(3).compile.toList.flatMap(IO.println)

    // Exercise #2
    def spaced[A](s: Stream[IO, A], d: FiniteDuration): Stream[IO, A] =
      Stream.fixedDelay[IO](d).zipRight(s)

    spaced(s, 1.second).take(3).compile.toList.flatMap(IO.println)

    val awakeDelayStream = Stream.awakeDelay[IO](2.second)
    awakeDelayStream.take(5).compile.toList.flatMap(IO.println)
    awakeDelayStream.zip(s).take(3).compile.toList.flatMap(IO.println)

    val awakeEveryStream = Stream.awakeEvery[IO](2.second)
    awakeEveryStream.take(5).compile.toList.flatMap(IO.println)
    awakeEveryStream.zip(s).take(3).compile.toList.flatMap(IO.println)
  }
}
