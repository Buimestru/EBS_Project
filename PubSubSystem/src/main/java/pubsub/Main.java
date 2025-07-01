package pubsub;

import pubsub.broker.Broker;
import pubsub.broker.Broker.Peer;
import pubsub.generator.Config;
import pubsub.generator.PublicationGenerator;
import pubsub.generator.SubscriptionGenerator;
import pubsub.model.Subscription;
import pubsub.model.Subscription.Filter;
import pubsub.model.Subscription.Operator;
import pubsub.publisher.Publisher;
import pubsub.subscriber.Subscriber;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1) Pornim 3 brokeri
        Broker b1 = new Broker(5000, 6000, List.of(
                new Peer("localhost", 5001),
                new Peer("localhost", 5002)
        ));
        Broker b2 = new Broker(5001, 6001, List.of(
                new Peer("localhost", 5000),
                new Peer("localhost", 5002)
        ));
        Broker b3 = new Broker(5002, 6002, List.of(
                new Peer("localhost", 5000),
                new Peer("localhost", 5001)
        ));
        b1.start();
        b2.start();
        b3.start();

        // 2) Definim subscriieri multiple pentru fiecare subscriber
        List<Subscription> subsS1 = SubscriptionGenerator.generateAllSubscriptions("S1");
        SubscriptionGenerator.writeToFile(subsS1, "data/subscriptions_s1.txt");
        List<Subscription> subsS2 = SubscriptionGenerator.generateAllSubscriptions("S2");
        SubscriptionGenerator.writeToFile(subsS2, "data/subscriptions_s2.txt");
        List<Subscription> subsS3 = SubscriptionGenerator.generateAllSubscriptions("S3");
        SubscriptionGenerator.writeToFile(subsS3, "data/subscriptions_s3.txt");

        // 3) Pornim câte un thread pentru fiecare subscriber
        new Thread(() -> Subscriber.run("S1", 7001, "localhost", 6000, subsS1)).start();
        new Thread(() -> Subscriber.run("S2", 7002, "localhost", 6001, subsS2)).start();
        new Thread(() -> Subscriber.run("S3", 7003, "localhost", 6002, subsS3)).start();

        // 4) După ce brokerii și subscriberii sunt gata, pornim publisher-ul
        String host       = "localhost";
        int port          = 5000;
        long intervalMs   = 100;
        Publisher publisher = new Publisher(host, port, intervalMs);
        publisher.publish();
/*
        System.setProperty("numThreads", "1");
        long start = System.currentTimeMillis();
        List<String> publications = PublicationGenerator.generatePublications(Config.TOTAL_PUBLICATIONS);
        PublicationGenerator.writeToFile(publications, "data/publications_v2.txt");

        List<String> subscriptions = SubscriptionGenerator.generateAllSubscriptions();
        SubscriptionGenerator.writeToFile(subscriptions, "data/subscriptions_v2.txt");
        long end = System.currentTimeMillis();
        System.out.printf("1 Thread: Generated %d publications and %d subscriptions in %d ms%n",
                Config.TOTAL_PUBLICATIONS, Config.TOTAL_SUBSCRIPTIONS, (end - start));
*/
        /*
        List<Publication> publications = PublicationGenerator.generatePublications(Config.TOTAL_PUBLICATIONS);
        System.out.println("Publications: " + publications.size());
        PublicationGenerator.writeToFile(publications, "data/publications_v3.txt");
        */
    }
}
