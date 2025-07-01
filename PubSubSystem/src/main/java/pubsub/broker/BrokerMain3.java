package pubsub.broker;

import java.util.List;

public class BrokerMain3 {
    public static void main(String[] args) throws Exception {
        // 1) Pornim 3 brokeri
        /*
        Broker b3 = new Broker(5002, 6002, List.of(
                new Broker.Peer("localhost", 5000),
                new Broker.Peer("localhost", 5001)
        ));*/

        Broker b3 = new Broker(5002, 6002, List.of());

        b3.start();
    }
}
