package pipelines

import cats.effect._
import fs2.Stream.ToPull
import fs2._
import fs2.io.file.{Files, Path}

object Pulls extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s3 = Stream(1, 2) ++ Stream(3) ++ Stream(4, 5)

    // IO is the effect type
    // Int is the output type  -> what type of elements are being emitted?
    // Unit is the result type -> what does the pull return after successful termination?
    //                         -> useful because is monadic
    val outputPull: Pull[IO, Int, Unit] = Pull.output1(1)
    outputPull.stream.compile.toList.flatMap(IO.println)

    val outputChunk: Pull[IO, Int, Unit] = Pull.output(Chunk(1,2,3))
    outputChunk.stream.compile.toList.flatMap(IO.println)

    val donePull: Pull[Pure, Nothing, Unit] = Pull.done

    val purePull: Pull[IO, Nothing, Int] = Pull.pure[IO, Int](5)

    val combined: Pull[IO, Int, Unit] =
      for {
        _ <- Pull.output1(1)
        _ <- Pull.output(Chunk(2,3,4))
      } yield ()
    combined.stream.compile.toList.flatMap(IO.println)
    combined.stream.chunks.compile.toList.flatMap(IO.println)

    val toPull: ToPull[Pure, Int] = s3.pull // provides interface for making pulls
    val echoPull: Pull[Pure, Int, Unit] = s3.pull.echo
    val takePull: Pull[Pure, Int, Option[Stream[Pure, Int]]] = s3.pull.take(5)
    val dropPull: Pull[Pure, Int, Option[Stream[Pure, Int]]] = s3.pull.drop(5)

    // Exercise
    def skipLimit[A](skip: Int, limit: Int)(s: Stream[IO, A]): Stream[IO, A] = {
        val p =
          for {
            tailOpt <- s.pull.drop(skip)
            _       <- tailOpt match {
                         case Some(rest) => rest.pull.take(limit)
                         case None       => Pull.done
                       }
          } yield ()
        p.stream
    }
    skipLimit(10, 10)(Stream.range(1, 100)).compile.toList.flatMap(IO.println)
    skipLimit(3, 15)(Stream.range(1, 5)).compile.toList.flatMap(IO.println)

    val unconsedRange: Pull[Pure, Nothing, Option[(Chunk[Int], Stream[Pure, Int])]] = s3.pull.uncons
    def firstChunk[A](s: Stream[Pure, A]): Stream[Pure, A] = {
      s.pull.uncons.flatMap {
        case Some((chunk, restOfStream)) => Pull.output(chunk)
        case None                        => Pull.done
      }.stream
    }
    IO.println(firstChunk(s3).toList)
    IO.println(s3.through(firstChunk).toList)

    def drop[A](n: Int): Pipe[Pure, A, A] = s => {
      def go(s: Stream[Pure, A], n: Int): Pull[Pure, A, Unit] = {
        s.pull.uncons.flatMap {
          case Some((chunk, restOfStream)) =>
            if(chunk.size < n) go(restOfStream, n - chunk.size)
            else               Pull.output(chunk.drop(n)) >> restOfStream.pull.echo
          case None =>
            Pull.done
        }
      }
      go(s, n).stream
    }
    IO.println(s3.through(drop(-1)).toList)
    IO.println(Stream.empty.through(drop(-1)).toList)

    // Exercise
    def filter[A](p: A => Boolean): Pipe[Pure, A, A] = s => {
      def go(s: Stream[Pure, A]): Pull[Pure, A, Unit] = {
        s.pull.uncons.flatMap {
          case Some((chunk, restOfStream)) =>
            Pull.output(chunk.filter(p)) >> go(restOfStream)
          case None =>
            Pull.done
        }
      }
      go(s).stream
    }
    IO.println(s3.through(filter(_ % 2 == 1)).toList)

    // Emits after each processed chunk
    def runningSum: Pipe[Pure, Int, Int] = s => {
      s.scanChunksOpt(0) { acc =>
        Some { chunk =>
          val newState = chunk.foldLeft(0)(_ + _) + acc
          (newState, Chunk.singleton(newState))
        }
      }
    }
    IO.println(s3.through(runningSum).toList)

    // Exercise
    def runningMax: Pipe[Pure, Int, Int] = s => {
      s.scanChunksOpt(0) { acc =>
        Some { chunk =>
          val newState = chunk.foldLeft(0)(_ max _) max acc
          (newState, Chunk.singleton(newState))
        }
      }
    }
    IO.println(s3.through(runningMax).toList)
  }
}
