package pubsub.test;

import pubsub.Publication;

import java.io.DataInputStream;
import java.io.EOFException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestBroker {
    public static void main(String[] args) throws Exception {
        int port = 5000;
        ServerSocket server = new ServerSocket(port);
        System.out.println("TestBroker ascultă pe port " + port);

        while (true) {
            Socket client = server.accept();
            System.out.println("Client conectat: " + client);

            try (DataInputStream in = new DataInputStream(client.getInputStream())) {
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
                client.close();
            }
        }
    }
}
