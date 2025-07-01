package pubsub.broker;

import java.util.List;

public class BrokerMain {
    public static void main(String[] args) throws Exception {
        // 1) Pornim 3 brokeri
        /*Broker b1 = new Broker(5000, 6000, List.of(
                new Broker.Peer("localhost", 5001),
                new Broker.Peer("localhost", 5002)
        ));
        */

        Broker b1 = new Broker(5000, 6000, List.of(
                new Broker.Peer("localhost", 5001)
        ));
        b1.start();
    }
}
