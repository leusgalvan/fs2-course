import fs2._

val s1 = Stream(1,2,3)
val s2 = Stream(4,5,6)
val nats = Stream.iterate(1)(_ + 1)

nats.take(10).toList
nats.drop(10).take(10).toList

(s1 ++ s2).toList
(s1 ++ nats).take(10).toList
(nats ++ s1).take(10).toList

val doubled = s1.map(_ * 2)
val evens = nats.map(_ * 2)

doubled.toList
evens.take(10).toList

s1.flatMap(i => Stream(i, i+1)).toList
nats.flatMap(i => Stream(i, i+1)).take(10).toList
s2.flatMap(i => nats.drop(i)).take(10).toList

val odds = nats.filter(_ % 2 == 1)
odds.take(10).toList

val myStream = for {
  s1 <- nats
  s2 <- evens
  s3 <- odds
} yield (s1, s2, s3)

myStream.take(10).toList

nats.zip(evens).take(10).toList
nats.zip(s1).toList
s1.zip(nats).toList
Stream.constant(2).zipWith(nats)(_ * _).take(10).toList

val length = s1.fold(0) { case (res, _) => res + 1 }.toList
val sum = s1.fold(0) { case (res, i) => res + i }.toList

// Exercise #1
// Create the stream of all odd numbers using nats and map
nats.map(2 * _ - 1).take(10).toList

// Exercise #2
// Implement a repeat method which takes a Stream and repeats all its
// elements indefinitely
// E.g repeat(Stream(1,2,3)) is Stream(1, 2, 3, 1, 2, 3, 1, 2, 3, ...)
def repeat[A](s: Stream[Pure, A]): Stream[Pure, A] =
  s ++ repeat(s)

repeat(Stream(1,2,3)).take(10).toList

// Exercise #3
// Implement the unNone method which removes None elements from
// a stream of Options.
// E.g unNone(Stream(Some(1), None, Some(2))) is Stream(1, 2)
def unNone[A](s: Stream[Pure, Option[A]]): Stream[Pure, A] =
  for {
    elemOpt <- s
    elem <- Stream.fromOption(elemOpt)
  } yield elem

unNone(Stream(None, Some(2), None, None)).toList