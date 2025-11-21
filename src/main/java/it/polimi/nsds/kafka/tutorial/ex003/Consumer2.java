package it.polimi.nsds.kafka.tutorial.ex003;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;

public class Consumer2 {
    private static final String groupId = "c2";
    private static final String defaultTopic = "ex002";
    private static final String anotherTopic = "ex002_processed";

    private static final String serverAddr = "localhost:9092";
    private static final boolean autoCommit = true;
    private static final int autoCommitIntervalMs = 15000;

    // Default is "latest": try "earliest" instead
    private static final String offsetResetStrategy = "latest";

    // If this is set to true, the consumer might also read records
    // that come from aborted transactions
    ///  new consumer properties
    private static final boolean readUncommitted = false;

    /// new producer properties
    private static final String transactionalId = "myTransactionalId";


    public static void main(String[] args) {
        // If there are arguments, use the first as group and the second as topic.
        // Otherwise, use default group and topic.

        final Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(autoCommit));
        consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, String.valueOf(autoCommitIntervalMs));

        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetResetStrategy);

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        /// transactional consumer
        if (readUncommitted) {
            consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_uncommitted");
        } else {
            consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        }

        final Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(defaultTopic));


        /// transactional producer
        producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId);

        final KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
        ///  remember to start transactions
        producer.initTransactions();


        while (true) {
            final ConsumerRecords<String, String> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));

            for (final ConsumerRecord<String, String> record : records) {

                producer.beginTransaction();

                System.out.print("Consumer group: " + groupId + "\t");
                System.out.println("Partition: " + record.partition() +
                        "\tOffset: " + record.offset() +
                        "\tKey: " + record.key() +
                        "\tValue: " + record.value()
                );

                StringBuilder s = new StringBuilder();
                for (int i =0 ; i< record.value().length(); i++){
                    if(record.value().charAt(i) >= 'a' && record.value().charAt(i) <= 'z'  ){
                        s.append(record.value().charAt(i));
                    }else{
                        s.append(" ");
                    }

                }

                System.out.println("Writing " + s+ " to " + anotherTopic + " with offset " + record.offset());
                final ProducerRecord<String, String> modified_record =
                        new ProducerRecord<>(anotherTopic, record.key(), s.toString());
                final Future<RecordMetadata> future = producer.send(modified_record);

                Random generator = new Random();
                if (generator.nextInt() % 2 == 0) {
                    producer.commitTransaction();
                } else {
                    // If not flushed, aborted messages are deleted from the outgoing buffer
                    producer.flush();
                    producer.abortTransaction();
                }

            }


        }


    }
}