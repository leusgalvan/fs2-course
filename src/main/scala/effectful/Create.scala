package effectful

import fs2._
import cats.effect._

import scala.concurrent.duration._
import scala.util.Random

object Create extends IOApp.Simple {
  override def run: IO[Unit] = {
    val sEval: Stream[IO, Unit] = Stream.eval(IO.println("my first effectful stream!"))

    // Won't work:
    // sEval.toList

    // If I want the side effects and the result
    sEval.compile.toList

    // If I just want the side effects
    sEval.compile.drain

    val sExec = Stream.exec(IO.println("this returns unit"))
    sExec.compile.drain

    // Pure to effectful
    val fromPure: Stream[IO, Int] = Stream(1,2,3).covary[IO]
    fromPure.compile.toList.flatMap(IO.println)

    val randomInts = Stream.repeatEval(IO(Random.nextInt()))
    randomInts.take(10).compile.toList.flatMap(IO.println)

    val fromFoldable = Stream.evals(IO(List(1,2,3)))
    fromFoldable.compile.toList.flatMap(IO.println)

    val natsEval = Stream.iterateEval(1)(a => IO.println(s"Producing ${a+1}") *> IO(a+1))
    natsEval.take(10).compile.toList.flatMap(IO.println)

    val alphabet = Stream.unfoldEval('a') { c =>
      if (c == 'z' + 1) IO.println("Finishing...").as(None)
      else IO.println(s"Producing $c").as(Some(c, (c + 1).toChar))
    }
    alphabet.compile.toList.flatMap(IO.println)

    val neverEnding = Stream.never[IO]
    neverEnding.interruptAfter(2.seconds).compile.drain
  }
}
