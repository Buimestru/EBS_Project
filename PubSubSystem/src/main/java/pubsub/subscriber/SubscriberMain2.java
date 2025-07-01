package pubsub.subscriber;

import pubsub.generator.SubscriptionGenerator;
import pubsub.model.Subscription;

import java.util.List;

public class SubscriberMain2 {
    public static void main(String[] args) throws Exception {
        // 2) Definim subscriptii multiple pentru fiecare subscriber
        List<Subscription> subsS2 = SubscriptionGenerator.generateAllSubscriptions("S2");
        SubscriptionGenerator.writeToFile(subsS2, "data/subscriptions_s2.txt");

        // 3) Pornim câte un thread pentru fiecare subscriber
        new Thread(() -> Subscriber.run("S2", 7002, "localhost", 6001, subsS2)).start();
    }
}
