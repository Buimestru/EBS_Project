package pubsub.generator;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

import pubsub.Publication;

public class PublicationGenerator {
    private static final Logger logger = Logger.getLogger(PublicationGenerator.class.getName());
    private static final Random rand = new Random();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.MM.yyyy");

    public static List<Publication> generatePublications(int count) {
        List<Publication> publications = new ArrayList<>(count);
        ExecutorService executor = Executors.newFixedThreadPool(Config.NUM_THREADS);
        List<Future<List<Publication>>> futures = new ArrayList<>();

        int perThread = count / Config.NUM_THREADS;

        for (int i = 0; i < Config.NUM_THREADS; i++) {
            int finalCount = (i == Config.NUM_THREADS - 1) ? count - perThread * i : perThread;
            futures.add(executor.submit(() -> {
                List<Publication> localList = new ArrayList<>();
                for (int j = 0; j < finalCount; j++) {
                    Publication msg = Publication.newBuilder()
                            .setId(UUID.randomUUID().toString())
                            .setStationid(rand.nextInt(Config.MAX_STATION_ID))
                            .setCity(Config.CITY_VALUES.get(rand.nextInt(Config.CITY_VALUES.size())))
                            .setTemp(rand.nextInt(Config.MAX_TEMP))
                            .setRain(Math.round((Config.RAIN_MIN + rand.nextDouble() * (Config.RAIN_MAX - Config.RAIN_MIN)) * 10.0) / 10.0)
                            .setWind(rand.nextInt(Config.MAX_WIND))
                            .setDirection(Config.DIRECTION_VALUES.get(rand.nextInt(Config.DIRECTION_VALUES.size())))
                            .setDate(LocalDate.now().minusDays(rand.nextInt(Config.DAYS_DIFF)).format(formatter))
                            .build();
                    localList.add(msg);
                }
                return localList;
            }));
        }

        for (Future<List<Publication>> future : futures) {
            try {
                publications.addAll(future.get());
            } catch (Exception e) {
                logger.severe("Error in thread: " + e.getMessage());
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                System.err.println("Executor did not terminate in time. Forced shutdown.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            System.err.println("Awaiting termination was interrupted. Forced shutdown.");
        }
        return publications;
    }

    public static void writeToFile(List<Publication> publications, String filename) throws Exception {
        try (PrintWriter writer = new PrintWriter(filename)) {
            for (Publication p : publications) writer.println(p.toString());
        }
    }
}

