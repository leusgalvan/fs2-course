package communication

import fs2._
import fs2.concurrent._
import cats.effect._

import scala.concurrent.duration._

object Topics extends IOApp.Simple {
  override def run: IO[Unit] = {
    Stream.eval(Topic[IO, Int]).flatMap { topic =>
      val p = Stream.iterate(1)(_ + 1).covary[IO].through(topic.publish).drain
      val c1 = topic.subscribe(10).evalMap(i => IO.println(s"Read $i from c1")).drain
      val c2 = topic.subscribe(10).evalMap(i => IO.println(s"Read $i from c2")).metered(200.millis).drain
      Stream(p, c1, c2).parJoinUnbounded
    }.interruptAfter(3.seconds).compile.drain
  }
}