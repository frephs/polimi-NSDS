package it.polimi.nsds.kafka.tutorial.ex004;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BasicProducer {
    private static final String defaultTopic = "ex002";

    private static final int numMessages = 100000;
    private static final int waitBetweenMsgs = 500;
    private static final boolean waitAck = false;
    private static final int messageLength = 30;

    private static final String serverAddr = "localhost:9092";

    public static void main(String[] args) {
        // If there are no arguments, publish to the default topic
        // Otherwise publish on the topics provided as argument
        List<String> topics = args.length < 1 ?
                Collections.singletonList(defaultTopic) :
                Arrays.asList(args);

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());


        final KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        final Random r = new Random();

        StringBuilder alphabet = new StringBuilder();
        //Generate random string
        for (char a = 'a'; a <= 'z'; a++) {
            alphabet.append(a);
        }
        for (char a = 'A'; a <= 'Z'; a++) {
            alphabet.append(a);
        }

        for (int i = 0; i < numMessages; i++) {
            final String topic = topics.get(r.nextInt(topics.size()));
            final String key = "Key" + r.nextInt(1000);

            final StringBuilder message = new StringBuilder(messageLength);

            while (message.length()<messageLength){
                if(r.nextBoolean()){
                    message.append(alphabet.charAt(r.nextInt(alphabet.length())));
                }else{
                    message.append(alphabet.charAt(r.nextInt(alphabet.length())));
                }
            }


            System.out.println(
                    "Topic: " + topic +
                    "\tKey: " + key +
                    "\tValue: " + message
            );

            final ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message.toString());
            final Future<RecordMetadata> future = producer.send(record);

            if (waitAck) {
                try {
                    RecordMetadata ack = future.get();
                    System.out.println("Ack for topic " + ack.topic() + ", partition " + ack.partition() + ", offset " + ack.offset());
                } catch (InterruptedException | ExecutionException e1) {
                    e1.printStackTrace();
                }
            }//TODO CHange # partitions kafka settings

            try {
                Thread.sleep(waitBetweenMsgs);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        producer.close();
    }
}