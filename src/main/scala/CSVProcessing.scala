import java.io.{BufferedReader, FileReader, Reader}
import java.nio.file.{Paths, Files => JFiles}

import scala.jdk.CollectionConverters._
import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import fs2._
import scala.collection.mutable.ListBuffer
import scala.io.{Source, StdIn}
import scala.util.{Try, Using}

object CSVProcessing extends IOApp.Simple {
  case class LegoSet(id: String, name: String, year: Int, themeId: Int, numParts: Int)

  def parseLegoSet(line: String): Option[LegoSet] = {
    val splitted = line.split(",")
    Try(LegoSet(
      id = splitted(0),
      name = splitted(1),
      year = splitted(2).toInt,
      themeId = splitted(3).toInt,
      numParts = splitted(4).toInt
    )).toOption
  }

  def readLegoSetImperative(filename: String, p: LegoSet => Boolean = _ => true, limit: Int = 10000): List[LegoSet] = {
    var reader: BufferedReader = null
    val legoSets: ListBuffer[LegoSet] = ListBuffer.empty
    var counter = 0
    try {
      reader = new BufferedReader(new FileReader(filename))
      var line: String = reader.readLine()
      while(line != null && counter < limit) {
        val legoSet = parseLegoSet(line)
        legoSet.filter(p).foreach { s =>
          legoSets.append(s)
          counter += 1
        }
        line = reader.readLine()
      }
    } finally {
        reader.close()
    }

    legoSets.toList
  }

  def readLegoSetEager(filename: String, p: LegoSet => Boolean = _ => true, limit: Int = Int.MaxValue): List[LegoSet] = {
    JFiles
      .readAllLines(Paths.get(filename))
      .asScala
      .flatMap(parseLegoSet)
      .filter(p)
      .take(limit)
      .toList
  }

  def readLegoSetIterator(filename: String, p: LegoSet => Boolean = _ => true, limit: Int = Int.MaxValue): List[LegoSet] = {
    Using(Source.fromFile(filename)) { source =>
      source
        .getLines()
        .flatMap(parseLegoSet)
        .filter(p)
        .take(limit)
        .toList
    }.get
  }

  def readLegoSetStreams(filename: String, p: LegoSet => Boolean = _ => true, limit: Int = Int.MaxValue): IO[List[LegoSet]] = {
    Files[IO].readAll(Path(filename))
      .through(text.utf8.decode)
      .through(text.lines)
      .parEvalMapUnbounded(s => IO(parseLegoSet(s)))
      .unNone
      .filter(p)
      .take(limit)
      .compile
      .toList
  }

  override def run: IO[Unit] = {
    val filename = "sets.csv"
    readLegoSetStreams(filename)
      .map(sets => println(sets))
//    IO(readLegoSetEager(filename))
//      .map(sets => println(sets))
//    IO(readLegoSetImperative(filename, _.numParts > 4000, 2))
//      .map(sets => println(sets))
  }
}
