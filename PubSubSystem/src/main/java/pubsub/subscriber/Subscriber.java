package pubsub.subscriber;

import pubsub.Publication;
import pubsub.model.Subscription;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Subscriber {
    public static void run(
            String id,
            int listenPort,
            String brokerHost,
            int brokerSubPort,
            List<Subscription> subs
    ) {
        // 1) Pornim thread-ul de notificări, care ascultă o singură conexiune
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(listenPort);
                 Socket sock = server.accept();
                 DataInputStream in = new DataInputStream(sock.getInputStream())) {
                System.out.println("[" + id + "] Conexiune de notificări acceptată");
                while (true) {
                    int len = in.readInt();
                    byte[] buf = new byte[len];
                    in.readFully(buf);
                    System.out.println("[" + id + "] Notificare: "
                            + pubsub.Publication.parseFrom(buf));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "NotifyThread-" + id).start();

        // 2) Așteptăm puțin ca ServerSocket-ul să fie gata
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        // 3) Trimitem subscripțiile
        try (Socket s = new Socket(brokerHost, brokerSubPort);
             ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream())) {
            for (Subscription sub : subs) {
                if (sub.getSubscriberId().equals(id)) {
                    oos.writeObject(sub);
                    oos.flush();
                    System.out.println(id + " a înregistrat " + sub);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
