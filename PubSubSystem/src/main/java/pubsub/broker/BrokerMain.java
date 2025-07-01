package pubsub.broker;

import java.util.List;

public class BrokerMain {
    public static void main(String[] args) throws Exception {
        // 1) Pornim 3 brokeri
        /*Broker b1 = new Broker(5000, 6000, List.of(
                new Broker.Peer("localhost", 5001),
                new Broker.Peer("localhost", 5002)
        ));
        Broker b2 = new Broker(5001, 6001, List.of(
                new Broker.Peer("localhost", 5000),
                new Broker.Peer("localhost", 5002)
        ));
        Broker b3 = new Broker(5002, 6002, List.of(
                new Broker.Peer("localhost", 5000),
                new Broker.Peer("localhost", 5001)
        ));*/

        Broker b1 = new Broker(5000, 6000, List.of(
                new Broker.Peer("localhost", 5001)
        ));
        Broker b2 = new Broker(5001, 6001, List.of(
                new Broker.Peer("localhost", 5002)
        ));
        Broker b3 = new Broker(5002, 6002, List.of());
        b1.start();
        b2.start();
        b3.start();
    }
}
