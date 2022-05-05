package concurrency

import fs2._
import fs2.concurrent._
import cats.effect._
import cats.effect.implicits._

import scala.util.Random
import scala.concurrent.duration._

object Merge extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1Infinite = Stream.iterate("0")(_ + "1").covary[IO].metered(100.millis)
    val s2Infinite = Stream.iterate("a")(_ + "a").covary[IO].metered(200.millis)
    val s3Infinite = s1Infinite.merge(s2Infinite)
    s3Infinite.interruptAfter(30.seconds).printlns.compile.drain

    val s1Failing = Stream("a", "b", "c").covary[IO].metered(100.millis) ++ Stream.raiseError[IO](new Exception("s1 failed"))
    val s3LeftFailing = s1Failing.merge(s2Infinite)
    s3LeftFailing.interruptAfter(30.seconds).printlns.compile.drain

    val s2Failing = Stream("a", "b", "c").covary[IO].metered(100.millis) ++ Stream.raiseError[IO](new Exception("s2 failed"))
    val s3RightFailing = s1Infinite.merge(s2Failing)
    s3RightFailing.interruptAfter(30.seconds).printlns.compile.drain

    val s1Finite = Stream(1, 2, 3).covary[IO].metered(100.millis)
    val s2Finite = Stream(4, 5, 6).covary[IO].metered(200.millis)
    val s3Finite = s1Finite.merge(s2Finite)
    s3Finite.interruptAfter(30.seconds).printlns.compile.drain

    val s3LeftHalts = s1Finite.mergeHaltL(s2Infinite)
    s3LeftHalts.interruptAfter(30.seconds).printlns.compile.drain

    // Exercise
    def fetchRandomQuoteFromSource1: IO[String] = {
      IO(Random.nextString(5))
    }

    def fetchRandomQuoteFromSource2: IO[String] = {
      IO(Random.nextString(25))
    }

    val s1 = Stream.repeatEval(fetchRandomQuoteFromSource1).take(100)
    val s2 = Stream.repeatEval(fetchRandomQuoteFromSource2).take(150)
    s1.merge(s2).interruptAfter(30.seconds).printlns.compile.drain
  }
}
