package concurrency

import fs2._
import cats.effect._
import scala.concurrent.duration._

object Concurrently extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1 = Stream(1, 2, 3).covary[IO].printlns
    val s2 = Stream(4, 5, 6).covary[IO].printlns
    s1.concurrently(s2).compile.drain

    val s1Inf = Stream.iterate(0)(_ + 1).covary[IO].printlns
    s1Inf.concurrently(s2).interruptAfter(3.seconds).compile.drain

    val s2Inf = Stream.iterate(2000)(_ + 1).covary[IO].printlns
    s1.concurrently(s2Inf).compile.drain

    val s1Failing = Stream.repeatEval(IO(42)).take(500).printlns ++ Stream.raiseError[IO](new Exception("s1 failed"))
    val s2Failing = Stream.repeatEval(IO(6000)).take(500).printlns ++ Stream.raiseError[IO](new Exception("s2 failed"))
    s1Inf.concurrently(s2Failing).compile.drain
    s1Failing.concurrently(s2Inf).compile.drain

    val s3 = Stream.iterate(3000)(_ + 1).covary[IO]
    val s4 = Stream.iterate(4000)(_ + 1).covary[IO]
    s3.concurrently(s4).take(100).compile.toList.flatMap(IO.println)
    s3.merge(s4).take(100).compile.toList.flatMap(IO.println)
    Stream(s3, s4).parJoinUnbounded.take(100).compile.toList.flatMap(IO.println)

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

    // Create a stream that emits a ref (initially 0)
    // Run the processor and the progressTracker concurrently
    Stream.eval(Ref.of[IO, Int](0)).flatMap { itemsProcessed =>
      processor(itemsProcessed).concurrently(progressTracker(itemsProcessed))
    }.compile.drain
  }
}