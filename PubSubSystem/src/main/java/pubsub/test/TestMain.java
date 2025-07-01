package pubsub.test;

import pubsub.generator.SubscriptionGenerator;
import pubsub.model.Subscription;
import pubsub.publisher.Publisher;
import pubsub.subscriber.Subscriber;

import java.util.List;

public class TestMain {
    public static void main(String[] args) throws Exception {
        String host       = "localhost";
        int port          = 5000;
        long intervalMs   = 100;


        Publisher publisher = new Publisher(host, port, intervalMs);
        publisher.publish();


        // Definim subscriptii multiple pentru fiecare subscriber
        List<Subscription> subsS1 = SubscriptionGenerator.generateAllSubscriptions("S1");
        SubscriptionGenerator.writeToFile(subsS1, "data/subscriptions_s1.txt");
        List<Subscription> subsS2 = SubscriptionGenerator.generateAllSubscriptions("S2");
        SubscriptionGenerator.writeToFile(subsS2, "data/subscriptions_s2.txt");
        List<Subscription> subsS3 = SubscriptionGenerator.generateAllSubscriptions("S3");
        SubscriptionGenerator.writeToFile(subsS3, "data/subscriptions_s3.txt");

        // Pornim câte un thread pentru fiecare subscriber
        new Thread(() -> Subscriber.run("S1", 7001, "localhost", 6000, subsS1)).start();
        new Thread(() -> Subscriber.run("S2", 7002, "localhost", 6000, subsS2)).start();
        new Thread(() -> Subscriber.run("S3", 7003, "localhost", 6000, subsS3)).start();
    }
}
