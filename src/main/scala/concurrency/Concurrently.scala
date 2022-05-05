package concurrency

import fs2._
import cats.effect._
import scala.concurrent.duration._

object Concurrently extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1 = Stream(1, 2, 3).covary[IO].printlns
    val s2 = Stream(4, 5, 6).covary[IO].printlns
    s1.concurrently(s2).compile.drain

    val s1Infinite = Stream.iterate(0)(_ + 1).covary[IO].printlns
    s1Infinite.concurrently(s2).interruptAfter(3.seconds).compile.drain

    val s2Infinite = Stream.iterate(1000000)(_ + 1).covary[IO].printlns
    s1.concurrently(s2Infinite).compile.drain

    val s1Failing = Stream.repeatEval(IO.pure(42)).take(500).printlns ++ Stream.raiseError[IO](new Exception("s1 failed"))
    val s2Failing = Stream.repeatEval(IO.pure(100000)).take(500).printlns ++ Stream.raiseError[IO](new Exception("s2 failed"))
    s1Infinite.concurrently(s2Failing).compile.drain
    s1Failing.concurrently(s2Infinite).compile.drain

    // Exercise
    val numItems = 30

    def processor(itemsProcessed: Ref[IO, Int]) =
      Stream
        .repeatEval(itemsProcessed.update(_ + 1))
        .take(numItems)
        .metered(100.millis)
        .drain

    def progressTracker(itemsProcessed: Ref[IO, Int]) =
      Stream
        .repeatEval(itemsProcessed.get.flatMap(n => IO.println(s"Progress: ${n * 100 / numItems} %")))
        .metered(100.millis)
        .drain

    Stream.eval(Ref.of[IO, Int](0)).flatMap { itemsProcessed =>
      processor(itemsProcessed).concurrently(progressTracker(itemsProcessed))
    }.compile.drain
  }
}
