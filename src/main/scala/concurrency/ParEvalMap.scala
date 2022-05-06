package concurrency

import fs2._
import cats.effect._
import scala.concurrent.duration._

object ParEvalMap extends IOApp.Simple {
  override def run: IO[Unit] = {
    trait JobState
    case object Created extends JobState
    case object Processed extends JobState

    case class Job(id: Long, state: JobState)

    def processJob(job: Job): IO[Job] = {
      IO.println(s"Processing job ${job.id}") *>
      IO.sleep(1.second) *>
      IO.pure(job.copy(state = Processed))
    }

    val jobs = Stream.unfold(1)(id => Some(Job(id, Created), id + 1)).covary[IO]
    jobs
      //.parEvalMapUnbounded(processJob)
      //.parEvalMap(5)(processJob)
      .parEvalMapUnordered(5)(processJob)
      .interruptAfter(3.seconds)
      .compile
      .toList
      .flatMap(IO.println)

    // Exercise
    implicit class RichStream[A](s: Stream[IO, A]) {
      def parEvalMapSeq[B](maxConcurrent: Int)(f: A => IO[List[B]]): Stream[IO, B] = {
        s.parEvalMap(maxConcurrent)(f).flatMap(Stream.emits)
      }

      def parEvalMapSeqUnbounded[B](f: A => IO[List[B]]): Stream[IO, B] = {
        parEvalMapSeq(Int.MaxValue)(f)
      }
    }

    case class Event(jobId: Long, seqNo: Long)

    def processJobS(job: Job): IO[List[Event]] = {
      IO.println(s"Processing job ${job.id}") *>
      IO.sleep(1.second) *>
      IO.pure(List.range(1, 10).map(seqNo => Event(job.id, seqNo)))
    }

    jobs
      //.parEvalFlatMap(5)(processJobS)
      .parEvalMapSeqUnbounded(processJobS)
      .interruptAfter(3.seconds)
      .compile
      .toList
      .flatMap(IO.println)
  }
}
