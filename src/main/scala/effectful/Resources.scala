package effectful

import java.io.{BufferedReader, FileReader}

import fs2._
import cats.effect._

object Resources extends IOApp.Simple {
  override def run: IO[Unit] = {
    //val r = Resource.fromAutoCloseable(IO.blocking(new BufferedReader(new FileReader("sets.csv"))))
    val acquireReader = IO.blocking(new BufferedReader(new FileReader("sets.csv")))
    val releaseReader = (reader: BufferedReader) => IO.println("Releasing...") *> IO.blocking(reader.close())

    // Exercise
    val readLines = (reader: BufferedReader) => {
      Stream
        .repeatEval(IO.blocking(reader.readLine()))
        .takeWhile(_ != null)
      //Stream.raiseError[IO](new Exception("boom"))
    }

    Stream
      //.bracket(acquireReader)(releaseReader)
      //.fromAutoCloseable(acquireReader)
      .resource(Resource.make(acquireReader)(releaseReader))
      .flatMap(readLines)
      .take(10)
      .printlns
      .compile
      .drain
  }
}
