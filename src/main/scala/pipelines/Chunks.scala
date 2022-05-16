package pipelines

import fs2._
import cats.effect._
import fs2.io.file._

import scala.reflect.ClassTag

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

    // Fast concat
    // Fast indexing
    // Avoids copying
    // Reduced List-like interface
    val c: Chunk[Int] = Chunk(1, 2, 3)
    val c2: Chunk[Int] = Chunk.array(Array(4, 5, 6))
    val c3: Chunk[Int] = Chunk.singleton(7)
    val c4: Chunk[Int] = Chunk.empty
    val a: Array[Int] = new Array(3)
    IO.println(c)
    IO.println(c.size)
    IO { c.copyToArray(a); println(a.toList) }
    IO.println(c ++ c2 ++ c3 ++ c4)
    IO.println(c(2))
    IO.println((c ++ c2 ++ c3 ++ c4).compact)
    IO.println(c.map(_ * 2))
    IO.println(c.flatMap(i => Chunk(i, i+1)))
    IO.println(c.filter(_ < 3))
    IO.println(c.take(5))

    // Exercise:
    def compact[A: ClassTag](chunk: Chunk[A]): Chunk[A] = {
      val arr = new Array[A](chunk.size)
      chunk.copyToArray(arr)
      Chunk.array(arr)
    }
    IO.println(compact(c ++ c2 ++ c3 ++ c4))
  }
}
