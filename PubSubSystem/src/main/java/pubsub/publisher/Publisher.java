package pubsub.publisher;

import pubsub.Publication;  // mesajul Protobuf generat
import pubsub.generator.Config;
import pubsub.generator.PublicationGenerator;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Publisher {
    private final String brokerHost;
    private final int brokerPort;
    private final long intervalMillis;

    public Publisher(String brokerHost, int brokerPort, long intervalMillis) {
        this.brokerHost     = brokerHost;
        this.brokerPort     = brokerPort;
        this.intervalMillis = intervalMillis;
    }

    public void publish() throws Exception {
        List<Publication> publications = PublicationGenerator.generatePublications(Config.TOTAL_PUBLICATIONS);
        //PublicationGenerator.writeToFile(publications, "data/publications_v3.txt");

        try (Socket socket = new Socket(brokerHost, brokerPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            for (Publication msg : publications) {
                // length-prefix framing
                byte[] data = msg.toByteArray();
                out.writeInt(data.length);
                out.write(data);
                out.flush();

                TimeUnit.MILLISECONDS.sleep(intervalMillis);
            }
        }
    }
}
