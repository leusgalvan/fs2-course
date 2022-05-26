package effectful

import java.io.{BufferedReader, FileReader}

import fs2._
import cats.effect._

object Resources extends IOApp.Simple {
  override def run: IO[Unit] = {
    val acquireReader = IO.blocking(new BufferedReader(new FileReader("sets.csv")))
    val releaseReader = (reader: BufferedReader) => IO.println("Releasing") *> IO.blocking(reader.close())

    def readLines(reader: BufferedReader): Stream[IO, String] = {
      Stream
        .repeatEval(IO.blocking(reader.readLine()))
        .takeWhile(_ != null)
      // Stream.raiseError[IO](new Exception("boom"))
    }

    val readerResource: Resource[IO, BufferedReader] = Resource.make(acquireReader)(releaseReader)

    Stream
      .fromAutoCloseable(acquireReader)
      //.resource(readerResource)
      .flatMap(readLines)
      .take(10)
      .printlns
      .compile
      .drain
  }
}