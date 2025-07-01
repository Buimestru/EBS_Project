package pubsub.test;

import pubsub.Publication;
import pubsub.model.Subscription;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TestBroker {
    public static void main(String[] args) throws Exception {

        int pubPort = 5000;
        ServerSocket pubServer = new ServerSocket(pubPort);
        System.out.println("TestBroker ascultă pe port " + pubPort);

        int subPort = 6000;
        ServerSocket subServer = new ServerSocket(subPort);
        System.out.println("TestBroker ascultă pe port " + subPort);

        while (true) {
            Socket pubClient = pubServer.accept();
            System.out.println("Client conectat: " + pubClient);

            try (DataInputStream in = new DataInputStream(pubClient.getInputStream())) {
                while (true) {
                    try {
                        // citim length prefix
                        int len = in.readInt();
                        byte[] buf = new byte[len];
                        in.readFully(buf);
                        // parse Protobuf
                        Publication pub = Publication.parseFrom(buf);
                        System.out.println("Am primit: " + pub);
                    } catch (EOFException eof) {
                        // clientul a închis conexiunea
                        System.out.println("Conexiunea clientului s-a închis.");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pubClient.close();
            }

            Socket subClient = subServer.accept();
            System.out.println("Client conectat: " + subClient);

            try (ObjectInputStream in = new ObjectInputStream(subClient.getInputStream())) {
                while (true) {
                    try {
                        Subscription sub = (Subscription) in.readObject();
                        System.out.println("Am primit: " + sub.toString());
                    } catch (EOFException eof) {
                        // clientul a închis conexiunea
                        System.out.println("Conexiunea clientului s-a închis.");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                subClient.close();
            }
        }

    }
}
