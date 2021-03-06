package fs2.kafka

import cats.effect.IO
import cats.implicits._
import fs2.{Chunk, Stream}

final class KafkaProducerSpec extends BaseKafkaSpec {

  describe("creating producers") {
    it("should support defined syntax") {
      val settings =
        ProducerSettings[IO, String, String]

      KafkaProducer.resource[IO, String, String](settings)
      KafkaProducer.resource[IO].toString should startWith("ProducerResource$")
      KafkaProducer.resource[IO].using(settings)

      KafkaProducer.stream[IO, String, String](settings)
      KafkaProducer.stream[IO].toString should startWith("ProducerStream$")
      KafkaProducer.stream[IO].using(settings)
    }
  }

  it("should be able to produce records with single") {
    withKafka { (config, topic) =>
      createCustomTopic(topic, partitions = 3)
      val toProduce = (0 until 100).map(n => s"key-$n" -> s"value->$n")

      val produced =
        (for {
          settings <- Stream(producerSettings[IO](config))
          producer <- KafkaProducer.stream[IO].using(settings)
          _ <- Stream.eval(IO(producer.toString should startWith("KafkaProducer$")))
          records <- Stream.chunk(Chunk.seq(toProduce).map {
            case passthrough @ (key, value) =>
              ProducerRecords.one(ProducerRecord(topic, key, value), passthrough)
          })
          batched <- Stream
            .eval(producer.produce(records))
            .map(_.map(_.passthrough))
            .buffer(toProduce.size)
          passthrough <- Stream.eval(batched)
        } yield passthrough).compile.toVector.unsafeRunSync()

      produced should contain theSameElementsAs toProduce

      val consumed =
        consumeNumberKeyedMessagesFrom[String, String](topic, produced.size)

      consumed should contain theSameElementsAs produced
    }
  }

  it("should be able to produce records with multiple") {
    withKafka { (config, topic) =>
      createCustomTopic(topic, partitions = 3)
      val toProduce = (0 until 10).map(n => s"key-$n" -> s"value->$n").toList
      val toPassthrough = "passthrough"

      val produced =
        (for {
          producer <- KafkaProducer.stream[IO].using(producerSettings(config))
          records = ProducerRecords(toProduce.map {
            case (key, value) =>
              ProducerRecord(topic, key, value)
          }, toPassthrough)
          result <- Stream.eval(producer.produce(records).flatten)
        } yield result).compile.lastOrError.unsafeRunSync()

      val records =
        produced.records.map {
          case (record, _) =>
            record.key -> record.value
        }.toList

      assert(records == toProduce && produced.passthrough == toPassthrough)

      val consumed =
        consumeNumberKeyedMessagesFrom[String, String](topic, toProduce.size)

      consumed should contain theSameElementsAs toProduce
    }
  }

  it("should be able to produce zero records with multiple") {
    withKafka { (config, topic) =>
      createCustomTopic(topic, partitions = 3)
      val passthrough = "passthrough"

      val result =
        (for {
          producer <- KafkaProducer.stream[IO].using(producerSettings(config))
          records = ProducerRecords(Nil, passthrough)
          result <- Stream.eval(producer.produce(records).flatten)
        } yield result).compile.lastOrError.unsafeRunSync()

      assert(result.passthrough == passthrough)
    }
  }

  it("should be able to produce zero records with passthrough") {
    withKafka { (config, topic) =>
      createCustomTopic(topic, partitions = 3)
      val passthrough = "passthrough"

      val result =
        (for {
          producer <- KafkaProducer.stream[IO].using(producerSettings(config))
          result <- Stream.eval {
            producer.produce(ProducerRecords(Nil, passthrough)).flatten
          }
        } yield result).compile.lastOrError.unsafeRunSync()

      assert(result.passthrough == passthrough)
    }
  }

  it("should get metrics") {
    withKafka { (config, topic) =>
      createCustomTopic(topic, partitions = 3)

      val info =
        KafkaProducer
          .stream[IO]
          .using(producerSettings(config))
          .evalMap(_.metrics)

      val res =
        info
          .take(1)
          .compile
          .lastOrError
          .unsafeRunSync()

      assert(res.nonEmpty)
    }
  }
}
