package pipelines

import fs2._
import cats.effect._
import fs2.io.file._

object Chunks extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1 = Stream(1, 2, 3, 4)
    IO.println(s1.chunks.toList)

    val s2 = Stream(Stream(1, 2, 3), Stream(4, 5, 6)).flatten
    IO.println(s2.chunks.toList)

    val s3 = Stream(1, 2) ++ Stream(3) ++ Stream(4, 5)
    IO.println(s3.chunks.toList)

    val s4 = Stream.repeatEval(IO(42)).take(5)
    s4.chunks.compile.toList.flatMap(IO.println)

    val s5 = Files[IO].readAll(Path("sets.csv"))
    s5.chunks.map(_.size).compile.toList.flatMap(IO.println)
  }
}
