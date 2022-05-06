package concurrency

import java.time.{LocalDate, LocalDateTime}

import fs2._
import cats.effect._

import scala.concurrent.duration._

object Zip extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1 = Stream(1, 2, 3).covary[IO].metered(1.second)
    val s2 = Stream(4, 5, 6, 7).covary[IO].metered(100.millis)
    s1.zip(s2).printlns.compile.drain
    (s1 ++ Stream.raiseError[IO](new Exception("boom"))).zip(s2).printlns.compile.drain
    (s1.take(1) ++ Stream.raiseError[IO](new Exception("boom"))).zip(s2).printlns.compile.drain

    val s3 = Stream.repeatEval(IO(LocalDateTime.now())).evalTap(IO.println).metered(1.second)
    s3.interruptAfter(4.seconds).compile.drain

    val s4 = Stream.iterate(1)(_ + 1).covary[IO]
    s3.zipRight(s4).interruptAfter(5.seconds).compile.toList.flatMap(IO.println)

    // Exercise #1: explain why
    val s5 = Stream.repeatEval(IO(LocalDateTime.now())).metered(1.second).printlns
    s3.zipRight(s5).interruptAfter(5.seconds).compile.toList.flatMap(IO.println)

    // Exercise #2: explain what happens
    val s6 = Stream.repeatEval(IO.println("Pulling left"))
    val s7 = Stream.repeatEval(IO.println("Pulling right"))
    s6.zip(s7).interruptAfter(5.seconds).compile.drain

    s6.parZip(s7).interruptAfter(5.seconds).compile.drain
  }
}
