package it.polimi.nsds.kafka.labs.evaluation25;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

// Group number: 28
// Group members: Francesco Genovese, Federico Grandi, Jonatan Sciaky

// Is it possible to have more than one partition for topics "sensors1" and "sensors2"?
// Yes, since messages of the same key always get sent to the same partition number (under the
// assumption that the assignment is deterministic and that the number of partitions of the two
// topics is the same).

// Is there any relation between the number of partitions in "sensors1" and "sensors2"?
// Yes: if the number of partitions of the two topics doesn't match we can't guarantee that messages
// of the same key from different topics will be processed by the same consumer.

// Is it possible to have more than one instance of Merger?
// Yes, as long as the number of partitions of the sensor topics are greater or equal to the number of
// Merger instances.

// If so, what is the relation between their group id?
// If they have different group ids the messages sent to the output topics will be duplicated, since
// they will all process the same messages. If their group ids are the same, they will read from different
// partitions instead. Both choices satisfy the "at least once semantics" requirement.

// Is it possible to have more than one partition for topic "merged"?
// Yes, since the messages have different keys and the load can be distributed between multiple partitions.

// Is it possible to have more than one instance of Validator?
// Yes, since it's possible to have multiple partitions for the "merged" topic.

// If so, what is the relation between their group id?
// In this case the group ids have to be the same, otherwise they wouldn't guarantee EOS.

public class Consumers28 {
    public static void main(String[] args) {
        String serverAddr = "localhost:9092";
        int stage = Integer.parseInt(args[0]);
        String groupId = args[1];
        // TODO: add arguments if necessary
        switch (stage) {
            case 0:
                new Merger(serverAddr, groupId).execute();
                break;
            case 1:
                new Validator(serverAddr, groupId).execute();
                break;
            case 2:
                System.err.println("Wrong stage");
        }
    }

    private static class Merger {
        private final String serverAddr;
        private final String consumerGroupId;

        private final Map<String, Integer> sensorsOneLastValues = new HashMap<>();
        private final Map<String, Integer> sensorsTwoLastValues = new HashMap<>();

        private static final String SENSORS_ONE_TOPIC = "sensors1";
        private static final String SENSORS_TWO_TOPIC = "sensors2";
        private static final String MERGED_TOPIC = "merged";

        public Merger(String serverAddr, String consumerGroupId) {
            this.serverAddr = serverAddr;
            this.consumerGroupId = consumerGroupId;
        }

        public void execute() {
            // Consumer
            final Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

            consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(false));
            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

            KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(List.of(SENSORS_ONE_TOPIC, SENSORS_TWO_TOPIC));

            // Producer
            final Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);

            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
            producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, String.valueOf(true));

            final KafkaProducer<String, Integer> producer = new KafkaProducer<>(producerProps);

            while (true) {
                final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));
                for (final ConsumerRecord<String, Integer> record : records) {
                    final String key = record.key();
                    final Integer value = record.value();

                    if (Objects.equals(record.topic(), SENSORS_ONE_TOPIC)) {
                        sensorsOneLastValues.put(key, value);
                    } else {
                        sensorsTwoLastValues.put(key, value);
                    }

                    final Integer sum = sensorsOneLastValues.getOrDefault(key, 0) + sensorsTwoLastValues.getOrDefault(key, 0);

                    System.out.println(sensorsOneLastValues.getOrDefault(key, 0) + " + " + sensorsTwoLastValues.getOrDefault(key, 0) + " = " + sum);

                    producer.send(new ProducerRecord<>(MERGED_TOPIC, key, sum));
                    producer.flush();
                    consumer.commitSync();
                }
            }
        }
    }

    private static class Validator {
        private final String serverAddr;
        private final String consumerGroupId;

        private static final String MERGED_TOPIC = "merged";
        private static final String OUTPUT_ONE_TOPIC = "output1";
        private static final String OUTPUT_TWO_TOPIC = "output2";

        public Validator(String serverAddr, String consumerGroupId) {
            this.serverAddr = serverAddr;
            this.consumerGroupId = consumerGroupId;
        }

        public void execute() {
            // Consumer
            final Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

            consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(false));
            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
            consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

            KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Collections.singleton(MERGED_TOPIC));

            // Producer
            final Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);

            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
            producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "validator");
            producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, String.valueOf(true));

            final KafkaProducer<String, Integer> producer = new KafkaProducer<>(producerProps);
            producer.initTransactions();

            while (true) {
                final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));
                producer.beginTransaction();

                for (final ConsumerRecord<String, Integer> record : records) {
                    final String key = record.key();
                    final Integer value = record.value();

                    producer.send(new ProducerRecord<>(OUTPUT_ONE_TOPIC, key, value));
                    producer.send(new ProducerRecord<>(OUTPUT_TWO_TOPIC, key, value));
                }

                final Map<TopicPartition, OffsetAndMetadata> map = new HashMap<>();
                for (final TopicPartition partition : records.partitions()) {
                    final List<ConsumerRecord<String, Integer>> partitionRecords = records.records(partition);
                    final long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                    map.put(partition, new OffsetAndMetadata(lastOffset + 1));
                }

                producer.sendOffsetsToTransaction(map, consumer.groupMetadata());
                producer.commitTransaction();
            }
        }
    }
}