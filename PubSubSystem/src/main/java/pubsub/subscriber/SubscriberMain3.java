package pubsub.subscriber;

import pubsub.generator.SubscriptionGenerator;
import pubsub.model.Subscription;

import java.util.List;

public class SubscriberMain3 {
    public static void main(String[] args) throws Exception {
        // 2) Definim subscriptii multiple pentru fiecare subscriber
        List<Subscription> subsS3 = SubscriptionGenerator.generateAllSubscriptions("S3");
        SubscriptionGenerator.writeToFile(subsS3, "data/subscriptions_s3.txt");

        // 3) Pornim câte un thread pentru fiecare subscriber
        new Thread(() -> Subscriber.run("S3", 7003, "localhost", 6002, subsS3)).start();
    }
}
