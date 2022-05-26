package concurrency

import fs2._
import cats.effect._

import java.time.LocalDateTime
import scala.concurrent.duration._

object Zip extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1 = Stream(1, 2, 3).covary[IO].metered(1.second)
    val s2 = Stream(4, 5, 6, 7).covary[IO].metered(100.millis)
    s1.zip(s2).printlns.compile.drain

    val s2Inf = Stream.iterate(0)(_ + 1).covary[IO].metered(200.millis)
    s1.zip(s2Inf).printlns.compile.drain

    val s1Failing = s1 ++ Stream.raiseError[IO](new Exception("s1 failed"))
    s1Failing.zip(s2).printlns.compile.drain

    val s2Failing = s2 ++ Stream.raiseError[IO](new Exception("s2 failed"))
    s1.zip(s2Failing).printlns.compile.drain
    s1.zip(s2).compile.toList.flatMap(IO.println)

    val s3 = Stream.repeatEval(IO(LocalDateTime.now)).evalTap(IO.println).metered(1.second)
    s3.zipRight(s2Inf).interruptAfter(3.seconds).compile.toList.flatMap(IO.println)

    val s4 = Stream.iterateEval(0)(i => IO.println(s"Pulling left $i").as(i + 1))
    val s5 = Stream.iterateEval(0)(i => IO.println(s"Pulling right $i").as(i + 1))
    s4.zip(s5).take(15).compile.toList.flatMap(IO.println)
    s4.parZip(s5).take(15).compile.toList.flatMap(IO.println)
  }
}