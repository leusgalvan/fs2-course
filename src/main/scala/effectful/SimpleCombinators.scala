package effectful

import fs2._
import cats.effect._
import cats.effect.implicits._

import scala.util.Random
import scala.concurrent.duration._

object SimpleCombinators extends IOApp.Simple {
  override def run: IO[Unit] = {
    val sample =
      for {
        n <- Stream.repeatEval(IO(Random.between(1, 37)).flatTap(i => IO.println(s"Generated $i"))).take(2)
        p <- Stream.iterateEval[IO, Int](n)(i => IO.println(s"Next: $i").as(i * 2)).take(5)
      } yield p
    sample.take(2).compile.toList.flatMap(IO.println)

    val concat = Stream.exec(IO.println("Starting")) ++ Stream(1,2,3) ++ Stream.exec(IO.println("Finishing"))
    concat.compile.toList.flatMap(IO.println)

    val delayed = Stream.sleep_[IO](1.second) ++ Stream.eval(IO.println("Emit"))
    delayed.compile.drain

    // Exercise
    def evalEvery[A](d: FiniteDuration)(fa: IO[A]): Stream[IO, A] = {
      (Stream.sleep_[IO](d) ++ Stream.eval(fa)).repeat
    }
    evalEvery(2.seconds)(IO.println("Hi").as(42)).take(10).compile.toList.flatMap(IO.println)
  }
}
