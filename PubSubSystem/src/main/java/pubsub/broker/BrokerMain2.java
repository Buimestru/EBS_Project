package pubsub.broker;

import java.util.List;

public class BrokerMain2 {
    public static void main(String[] args) throws Exception {
        // 1) Pornim 3 brokeri
        /*
        Broker b2 = new Broker(5001, 6001, List.of(
                new Broker.Peer("localhost", 5000),
                new Broker.Peer("localhost", 5002)
        ));
        */

        Broker b2 = new Broker(5001, 6001, List.of(
                new Broker.Peer("localhost", 5002)
        ));
        b2.start();
    }
}
