package pubsub.generator;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import pubsub.model.Subscription;
import pubsub.model.Subscription.Filter;
import pubsub.model.Subscription.Operator;

public class SubscriptionGenerator {
    private static final Logger logger = Logger.getLogger(SubscriptionGenerator.class.getName());

    public static List<Subscription> generateAllSubscriptions(String subscriberId) {
        int totalSubs = Config.TOTAL_SUBSCRIPTIONS;
        List<List<Filter>> allSubs = new ArrayList<>(totalSubs);
        for (int i = 0; i < totalSubs; i++) {
            allSubs.add(new ArrayList<>());
        }

        Random rand = new Random();

        // Construim subscriptiile conform procentelor
        for (Map.Entry<String, Integer> entry : Config.FIELD_FREQUENCIES.entrySet()) {
            String field = entry.getKey();
            int percent = entry.getValue();
            int fieldCount = Math.round((percent / 100.0f) * totalSubs);

            // Amestecăm indexii și alegem primii "fieldCount"
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < totalSubs; i++) indices.add(i);
            Collections.shuffle(indices);

            int equalityCount = Config.EQUALITY_OPERATOR_PERCENTAGES.getOrDefault(field, 0);
            int equalCount = Math.round((equalityCount / 100.0f) * fieldCount);
            int avgCriterionPercentage = Config.AVG_CRITERION_PERCENTAGES.getOrDefault(field, 0);
            int avgCount = Math.round((avgCriterionPercentage / 100.0f) * fieldCount);

            for (int i = 0; i < fieldCount; i++) {
                int subIndex = indices.get(i);
                String operator;
                if (i < equalCount) {
                    operator = "=";
                } else {
                    List<String> ops = new ArrayList<>(Config.OPERATORS);
                    if (field.equals("city") || field.equals("direction")) {
                        ops = new ArrayList<>(List.of("=", "!="));
                    }
                    //ops.remove("=");
                    operator = ops.get(rand.nextInt(ops.size()));
                }

                String value = generateValueForField(field, rand);

                String complexField;
                if (i < avgCount) {
                    complexField = "avg_" + field;
                }else{
                    complexField = field;
                }

                allSubs.get(subIndex).add(new Filter(complexField, Operator.fromSymbol(operator), value));
            }
        }

        // Găsim toate subscripțiile goale și redistribuim câte o condiție spre ele
        List<Integer> emptyIndexes = new ArrayList<>();
        List<Integer> donorIndexes = new ArrayList<>();

        for (int i = 0; i < totalSubs; i++) {
            if (allSubs.get(i).isEmpty()) {
                emptyIndexes.add(i);
            } else if (allSubs.get(i).size() > 1) {
                donorIndexes.add(i);
            }
        }

        // Pentru fiecare subscripție goală, mutăm o condiție de la una cu mai multe
        for (int emptyIdx : emptyIndexes) {
            if (donorIndexes.isEmpty()) break;
            int donorIdx = donorIndexes.removeFirst();
            List<Filter> donorList = allSubs.get(donorIdx);
            Filter subToMove = donorList.removeFirst();

            allSubs.get(emptyIdx).add(subToMove);

            // Dacă donorul mai are condiții, îl putem folosi din nou
            if (donorList.size() > 1) {
                donorIndexes.add(donorIdx);
            }
        }

        // Paralelizăm transformarea subscriptiilor în stringuri
        ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREADS);
        List<Future<List<Subscription>>> futures = new ArrayList<>();

        int perThread = totalSubs / Config.NUM_THREADS;
        for (int t = 0; t < Config.NUM_THREADS; t++) {
            int start = t * perThread;
            int end = (t == Config.NUM_THREADS - 1) ? totalSubs : start + perThread;
            futures.add(executor.submit(() -> {
                List<Subscription> localList = new ArrayList<>();
                for (int i = start; i < end; i++) {
                    List<Filter> filters = allSubs.get(i);
                    localList.add(new Subscription(subscriberId, filters));
                }
                return localList;
            }));
        }

        List<Subscription> result = new ArrayList<>(totalSubs);
        for (Future<List<Subscription>> future : futures) {
            try {
                result.addAll(future.get());
            } catch (Exception e) {
                logger.severe("Error in thread: " + e.getMessage());
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return result;
    }

    private static String generateValueForField(String field, Random rand) {
        return switch (field) {
            case "stationid" -> String.valueOf(rand.nextInt(Config.MAX_STATION_ID));
            case "city" -> "\"" + Config.CITY_VALUES.get(rand.nextInt(Config.CITY_VALUES.size())) + "\"";
            case "temp" -> String.valueOf(rand.nextInt(Config.MAX_TEMP));
            case "wind" -> String.valueOf(rand.nextInt(Config.MAX_WIND));
            case "rain" -> String.valueOf(Math.round((Config.RAIN_MIN + rand.nextDouble() * (Config.RAIN_MAX - Config.RAIN_MIN)) * 10.0) / 10.0);
            case "direction" -> "\"" + Config.DIRECTION_VALUES.get(rand.nextInt(Config.DIRECTION_VALUES.size())) + "\"";
            case "date" -> String.valueOf(LocalDate.now().minusDays(rand.nextInt(Config.DAYS_DIFF)));
            default -> "\"unknown\"";
        };
    }

    public static void writeToFile(List<Subscription> subscriptions, String filename) throws Exception {
        try (PrintWriter writer = new PrintWriter(filename)) {
            for (Subscription s : subscriptions) {
                writer.println(s.toString());
            }
        }
    }

    public static void printFieldFrequencies(List<String> subscriptions) {
        Map<String, Integer> fieldCounts = new HashMap<>();
        Map<String, Integer> equalityOperatorCounts = new HashMap<>();
        int totalSubscriptions = subscriptions.size();
        int emptySubscriptions = 0;

        for (String line : subscriptions) {
            line = line.substring(1, line.length() - 1); // elimină acoladele {}
            if (line.trim().isEmpty()) {
                emptySubscriptions++;
                continue;
            }

            String[] conditions = line.split(";");
            Set<String> fieldsInSubscription = new HashSet<>();

            for (String cond : conditions) {
                String[] parts = cond.replace("(", "").replace(")", "").split(",");
                if (parts.length >= 3) {
                    String field = parts[0].trim();
                    String operator = parts[1].trim();

                    fieldsInSubscription.add(field);

                    if (operator.equals("=") && Config.EQUALITY_OPERATOR_PERCENTAGES.containsKey(field)) {
                        equalityOperatorCounts.put(field, equalityOperatorCounts.getOrDefault(field, 0) + 1);
                    }
                }
            }

            for (String field : fieldsInSubscription) {
                fieldCounts.put(field, fieldCounts.getOrDefault(field, 0) + 1);
            }
        }

        System.out.println("\n📊 Frecvență câmpuri în subscriptii:");
        for (Map.Entry<String, Integer> entry : fieldCounts.entrySet()) {
            int count = entry.getValue();
            double percent = (count * 100.0) / totalSubscriptions;
            System.out.printf(" - %s: %d din %d (%.2f%%)\n", entry.getKey(), count, totalSubscriptions, percent);
        }

        System.out.println("\n📈 Frecvență operator '=' pe câmpuri:");
        for (String field : Config.EQUALITY_OPERATOR_PERCENTAGES.keySet()) {
            int equalCount = equalityOperatorCounts.getOrDefault(field, 0);
            int fieldCount = fieldCounts.getOrDefault(field, 0);
            double percent = fieldCount > 0 ? (equalCount * 100.0) / fieldCount : 0.0;
            System.out.printf(" - %s: %d din %d (%.2f%%)\n", field, equalCount, fieldCount, percent);
        }

        System.out.printf("\n🚫 Subscriptii goale: %d din %d (%.2f%%)\n", emptySubscriptions, totalSubscriptions,
                (emptySubscriptions * 100.0) / totalSubscriptions);
    }

}
