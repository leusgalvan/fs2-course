package effectful

import fs2._
import cats.effect._
import cats.implicits._

import scala.concurrent.duration._
import scala.util.Random

object Retry extends IOApp.Simple {
  override def run: IO[Unit] = {
    def doEffectFailing[A](io: IO[A]): IO[A] =
      IO(math.random()).flatMap { flag =>
        if(flag < 0.5) IO.println("Failing...") *> IO.raiseError(new Exception("boom"))
        else IO.println("Successsful!") *> io
      }

    Stream.retry(
      fo = doEffectFailing[Int](IO.pure(42)),
      delay = 1.second,
      nextDelay = _ + 1.second,
      maxAttempts = 5,
    ).compile.drain

    // Exercise
    val searches = Stream.iterateEval("")(s => IO(Random.nextPrintableChar()).map(s + _))
    def performSearch(text: String): IO[Unit] = doEffectFailing(IO.println(s"Performing search for text: $text"))
    def performSearchRetrying(text: String): Stream[IO, Unit] =
      Stream.retry(
        fo = performSearch(text),
        delay = 1.second,
        nextDelay = _ + 1.second,
        maxAttempts = 5,
      )

    searches
      .metered(200.millis)
      .debounce(1.second)
      .flatMap(performSearchRetrying)
      .interruptAfter(15.seconds)
      .compile
      .drain
  }
}
