package pubsub.generator;

import java.util.List;
import java.util.Map;

public class Config {
    // New config for system
    // windowing
    public static final int           WINDOW_SIZE        = 10;
    // Procentajul de utilizare al criteriului avg pentru câmpuri
    public static final Map<String, Integer> AVG_CRITERION_PERCENTAGES = Map.of(
            "temp", 80,
            "wind", 70
    );
    //----------------------

    public static final int TOTAL_PUBLICATIONS = 100;
    public static final int TOTAL_SUBSCRIPTIONS = 100;

    public static final int NUM_THREADS = Integer.parseInt(System.getProperty("numThreads", "4"));

    // Frecvența câmpurilor în subscriptii
    public static final Map<String, Integer> FIELD_FREQUENCIES = Map.of(
            "city", 90,
            "temp", 100,
            "wind", 100,
            "stationid", 30,
            "rain", 60,
            "direction", 20,
            "date", 80
    );

    // Procentajul de utilizare a operatorului "=" pentru câmpuri
    public static final Map<String, Integer> EQUALITY_OPERATOR_PERCENTAGES = Map.of(
            "city", 70
    );

    public static final List<String> CITY_VALUES = List.of("Bucharest", "Cluj", "Iasi", "Timisoara", "Suceava", "Botosani");
    public static final List<String> DIRECTION_VALUES = List.of("N", "S", "E", "W", "NE", "NW", "SE", "SW");
    public static final int MAX_STATION_ID = 1000;
    public static final int MAX_TEMP = 50;
    public static final int MAX_WIND = 150;
    public static final double RAIN_MIN = 0.0;
    public static final double RAIN_MAX = 100.0;
    public static final int DAYS_DIFF = 1000;
    public static final List<String> OPERATORS = List.of("=", "<", ">", "<=", ">=", "!=");
}

