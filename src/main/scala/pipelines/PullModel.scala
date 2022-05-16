package pipelines

object PullModel {
  /*
  Pull based stream (e.g. fs2 by default):
  - The stream contains a 'recipe' to produce more elements
  - The consumer of the stream asks the stream to produce the next
    group of elements (i.e it 'pulls' from the stream)
  - The stream emits the next set of elements (group of elements == chunk)
  - The consumer processes the chunk and decides whether to continue the process or stop
  - If the consumer stops, the stream stops emitting elements

  Push based stream (e.g. Rx)
  - The production rate/speed is set by the producer (often called an Observable)
  - The consumer (often called an Observer) provides a callback indicating how to handle elements as they come
  - You can unsuscribe from the producer if you want
   */
}
