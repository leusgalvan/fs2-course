package effectful

import fs2._
import cats.effect._
import cats.implicits._

import scala.concurrent.duration._

object Timed extends IOApp.Simple {
  override def run: IO[Unit] = {
    val drinkWater = Stream.iterateEval(1)(n => IO.println("Drink more water!").as(n+1))
    drinkWater.compile.drain

    drinkWater.timeout(1.second).compile.drain
    drinkWater.interruptAfter(1.second).compile.drain

    // Throttling
    drinkWater
      .meteredStartImmediately(1.second)
      .interruptAfter(5.seconds)
      .compile
      .toList
      .flatMap(IO.println)

    // Debounce
    drinkWater
      .debounce(1.second)
      .interruptAfter(5.seconds)
      .compile
      .toList
      .flatMap(IO.println)
  }
}
