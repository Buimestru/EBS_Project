package pubsub.subscriber;

import pubsub.generator.SubscriptionGenerator;
import pubsub.model.Subscription;

import java.util.List;

public class SubscriberMain {
    public static void main(String[] args) throws Exception {
        // 2) Definim subscriptii multiple pentru fiecare subscriber
        List<Subscription> subsS1 = SubscriptionGenerator.generateAllSubscriptions("S1");
        SubscriptionGenerator.writeToFile(subsS1, "data/subscriptions_s1.txt");

        // 3) Pornim câte un thread pentru fiecare subscriber
        new Thread(() -> Subscriber.run("S1", 7001, "localhost", 6000, subsS1)).start();
    }
}
