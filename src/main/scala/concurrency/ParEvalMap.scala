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

    val jobs: Stream[IO, Job] = Stream.unfold(1)(id => Some(Job(id, Created), id + 1)).covary[IO]
    jobs
      .parEvalMapUnordered(5)(processJob)
      //.parEvalMap(5)(processJob)
      //.parEvalMapUnbounded(processJob)
      //.evalMap(processJob)
      .interruptAfter(5.seconds)
      .compile
      .toList
      .flatMap(IO.println)

    // Exercise
    case class Event(jobId: Long, seqNo: Long)
    def processJobS(job: Job): IO[List[Event]] = {
      IO.println(s"Processing ${job.id}") *>
        IO.sleep(1.second) *>
        IO.pure(List.range(1, 10).map(seqNo => Event(job.id, seqNo)))
    }

    implicit class RichStream[A](s: Stream[IO, A]) {
      def parEvalMapSeq[B](maxConcurrent: Int)(f: A => IO[List[B]]): Stream[IO, B] =
        s.parEvalMap(maxConcurrent)(f).flatMap(Stream.emits)

      def parEvalMapSeqUnbounded[B](f: A => IO[List[B]]): Stream[IO, B] =
        parEvalMapSeq(Int.MaxValue)(f)
    }

    jobs
      .parEvalMapSeqUnbounded(processJobS)
      //.parEvalMapSeq(5)(processJobS)
      .interruptAfter(3.seconds)
      .compile
      .toList
      .flatMap(IO.println)
  }
}