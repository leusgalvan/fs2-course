package concurrency

import fs2._
import cats.effect._
import cats.effect.std.Queue

import scala.concurrent.duration._
import scala.util.Random

object Join extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1Finite = Stream(1,2,3).covary[IO].metered(100.millis)
    val s2Finite = Stream(4,5,6).covary[IO].metered(50.millis)
    val jFinite = Stream(s1Finite, s2Finite).parJoinUnbounded // == s1Finite.merge(s2Finite)
    jFinite.printlns.compile.drain

    val s3Infinite = Stream.iterate(3000000)(_ + 1).covary[IO].metered(50.millis)
    val s4Infinite = Stream.iterate(4000000)(_ + 1).covary[IO].metered(50.millis)
    val jAll = Stream(s1Finite, s2Finite, s3Infinite, s4Infinite).parJoinUnbounded
    jAll.printlns.interruptAfter(3.seconds).compile.drain

    val s1Failing = Stream(1,2,3).covary[IO].metered(100.millis) ++ Stream.raiseError[IO](new Exception("s1 failed"))
    val jFailingS1 = Stream(s1Failing, s2Finite, s3Infinite, s4Infinite).parJoinUnbounded
    jFailingS1.printlns.interruptAfter(30.seconds).compile.drain

    val s1Infinite = Stream.iterate(1000000)(_ + 1).covary[IO].metered(50.millis)
    val s2Infinite = Stream.iterate(2000000)(_ + 1).covary[IO].metered(50.millis)
    val jBounded2 = Stream(s1Infinite.take(50), s2Infinite.take(50), s3Infinite.take(50), s4Infinite.take(50)).parJoin(2)
    jBounded2.printlns.interruptAfter(5.seconds).compile.drain

    // Exercise
    def producer(queue: Queue[IO, Int]): Stream[IO, Nothing] =
      Stream.repeatEval(IO(Random.between(1, 1000)).flatMap(queue.offer)).drain

    def consumer(queue: Queue[IO, Int]): Stream[IO, Nothing] =
      Stream.repeatEval(queue.take).map(_ * 2).printlns

    Stream.eval(Queue.unbounded[IO, Int]).flatMap { queue =>
      val p = producer(queue)
      val cs = Stream.constant(consumer(queue)).take(10)
      val all = Stream(p) ++ cs
      all.parJoinUnbounded
    }.interruptAfter(5.seconds).compile.drain
  }
}
