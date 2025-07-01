package pubsub.broker;

import pubsub.Publication;
import pubsub.model.Subscription;
import pubsub.model.Subscription.Filter;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Broker {
    private final int pubPort, subPort;
    private final List<Peer> neighbors;
    private final List<Subscription> subs = new CopyOnWriteArrayList<>();
    private final Set<String> seenIds = ConcurrentHashMap.newKeySet();
    private final Map<Subscription, Deque<Publication>> windows = new ConcurrentHashMap<>();


    public Broker(int pubPort, int subPort, List<Peer> neighbors) {
        this.pubPort   = pubPort;
        this.subPort   = subPort;
        this.neighbors = neighbors;
    }

    public void start() throws IOException {
        // 1) Ascultă publicații
        ServerSocket ps = new ServerSocket(pubPort);
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try (Socket s = ps.accept();
                     DataInputStream in = new DataInputStream(s.getInputStream())) {
                    // citim mesaje multiple de pe aceeași conexiune
                    while (true) {
                        int len = in.readInt();
                        byte[] buf = new byte[len];
                        in.readFully(buf);
                        Publication pub = Publication.parseFrom(buf);
                        route(pub);
                    }
                } catch (EOFException eof) {
                    // conexiunea clientului s-a închis
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // 2) Ascultă subscrieri
        ServerSocket ss = new ServerSocket(subPort);
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try (Socket s = ss.accept();
                     ObjectInputStream ois = new ObjectInputStream(s.getInputStream())) {
                    Subscription sub = (Subscription) ois.readObject();
                    subs.add(sub);
                    boolean isComplex = sub.getFilters().stream()
                            .anyMatch(f -> f.getField().startsWith("avg_"));
                    if (isComplex) {
                        windows.put(sub, new ArrayDeque<>(BrokerConfig.WINDOW_SIZE));
                    }

                    System.out.println("Broker@" + subPort + " înregistrat sub: " + sub);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println("Broker pornit pe pubPort=" + pubPort + " subPort=" + subPort);
    }

    private void route(Publication pub) {
        // 1) deduplicare după ID
        if (!seenIds.add(pub.getId())) return;

        // 2) pentru fiecare subscripţie
        for (Subscription sub : subs) {
            // 2.a) match pe filtrele non-avg_
            if (!sub.getFilters().stream()
                    .filter(f -> !f.getField().startsWith("avg_"))
                    .allMatch(f -> matchField(pub, f.getField(), f.getOp().symbol, f.getValue()))) {
                continue;
            }

            boolean isWindowed = windows.containsKey(sub);
            if (!isWindowed) {
                // subscripţie simplă: notific instant
                notifySub(pub, sub.getSubscriberId());
            } else {
                // subscripţie complexă: tumbling window
                Deque<Publication> q = windows.get(sub);
                q.add(pub);
                if (q.size() < BrokerConfig.WINDOW_SIZE) {
                    continue;  // aşteptăm să umple fereastra
                }

                // 2.b) procesăm fiecare filtru avg_
                boolean allOk = true;
                for (Subscription.Filter f : sub.getFilters()) {
                    String fld = f.getField();
                    if (fld.startsWith("avg_")) {
                        String real = fld.substring(4);      // ex: "temp"
                        double threshold = Double.parseDouble(f.getValue());
                        // calculăm media pe fereastră
                        double avg = q.stream()
                                .mapToDouble(p -> switch (real) {
                                    case "temp" -> p.getTemp();
                                    case "wind" -> p.getWind();
                                    case "rain" -> p.getRain();
                                    case "stationid" -> p.getStationid();
                                    default -> throw new IllegalArgumentException(real);
                                })
                                .average()
                                .orElse(Double.NaN);
                        // comparăm (aici am presupus doar operatorul ">")
                        if (!(avg > threshold)) {
                            allOk = false;
                            break;
                        }
                    }
                }

                if (allOk) {
                    // meta-publicaţie: semnalăm conditionMet = true
                    Publication meta = Publication.newBuilder(pub)
                            .setConditionMet(true)
                            .build();
                    notifySub(meta, sub.getSubscriberId());
                }
                // tumbling: golim buffer-ul
                q.clear();
            }
        }

        // 3) forward în overlay
        for (Peer p : neighbors) {
            p.forward(pub);
        }
    }


    private boolean match(Publication pub, Subscription sub) {
        for (Filter f : sub.getFilters()) {
            String field = f.getField();
            String op    = f.getOp().symbol;
            String val   = f.getValue();
            if (!matchField(pub, field, op, val)) return false;
        }
        return true;
    }

    private boolean matchField(Publication pub, String field, String op, String val) {
        switch (field) {
            case "stationid": return cmp(pub.getStationid(), Integer.parseInt(val), op);
            case "city":      return cmp(pub.getCity(), val, op);
            case "temp":      return cmp(pub.getTemp(), Integer.parseInt(val), op);
            case "rain":      return cmp(pub.getRain(), Double.parseDouble(val), op);
            case "wind":      return cmp(pub.getWind(), Integer.parseInt(val), op);
            case "direction": return cmp(pub.getDirection(), val, op);
            case "date":      return cmp(pub.getDate(), val, op);
            default: return false;
        }
    }

    private boolean cmp(int a, int b, String op) {
        return switch (op) {
            case "="  -> a == b;
            case "!=" -> a != b;
            case ">"  -> a > b;
            case "<"  -> a < b;
            case ">=" -> a >= b;
            case "<=" -> a <= b;
            default   -> false;
        };
    }
    private boolean cmp(double a, double b, String op) {
        return switch (op) {
            case "="  -> a == b;
            case "!=" -> a != b;
            case ">"  -> a > b;
            case "<"  -> a < b;
            case ">=" -> a >= b;
            case "<=" -> a <= b;
            default   -> false;
        };
    }
    private boolean cmp(String a, String b, String op) {
        return switch (op) {
            case "="  -> a.equals(b);
            case "!=" -> !a.equals(b);
            default   -> false;
        };
    }

    private void notifySub(Publication pub, String subId) {
        int port = BrokerConfig.lookupPort(subId);
        try (Socket s = new Socket("localhost", port);
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {
            byte[] buf = pub.toByteArray();
            out.writeInt(buf.length);
            out.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Peer {
        private final String host; private final int port;
        public Peer(String host, int port) { this.host = host; this.port = port; }
        public void forward(Publication pub) {
            try (Socket s = new Socket(host, port);
                 DataOutputStream out = new DataOutputStream(s.getOutputStream())) {
                byte[] buf = pub.toByteArray();
                out.writeInt(buf.length);
                out.write(buf);
            } catch (IOException ignored) {}
        }
    }
}
