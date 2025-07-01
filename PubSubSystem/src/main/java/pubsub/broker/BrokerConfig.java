package pubsub.broker;

import java.util.Map;

public class BrokerConfig {
    private static final Map<String,Integer> map = Map.of(
            "S1", 7001,
            "S2", 7002,
            "S3", 7003
    );

    public static final int WINDOW_SIZE = 10;

    public static int lookupPort(String subscriberId) {
        return map.getOrDefault(subscriberId, 7001);
    }
}
