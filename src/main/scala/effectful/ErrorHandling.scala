package effectful

import fs2._
import cats.effect._

object ErrorHandling extends IOApp.Simple {
  override def run: IO[Unit] = {
    def doWork(i: Int): Stream[IO, Int] = {
      Stream.eval(IO(math.random())).flatMap { flag =>
        if(flag < 0.8) Stream.eval(IO.println(s"Processing $i").as(i))
        else           Stream.raiseError[IO](new Exception(s"Error while handling $i"))
      }
    }

    // Exercise
    implicit class RichStream[A](s: Stream[IO, A]) {
      def flatAttempt: Stream[IO, A] = {
        s.attempt.collect { case Right(v) => v }
      }
    }


    Stream
      .iterate(1)(_ + 1)
      .flatMap(doWork)
      .take(10)
      //.handleErrorWith(e => Stream.exec(IO.println(s"Recovering: ${e.getMessage}")))
      //.attempt
      .flatAttempt
      .compile
      .toList
      .flatMap(IO.println)
  }
}
