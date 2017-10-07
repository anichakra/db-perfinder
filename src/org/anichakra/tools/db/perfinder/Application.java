package org.anichakra.tools.db.perfinder;

import java.io.Console;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.anichakra.tools.db.perfinder.rdbms.RdbmsPerfinder;

public class Application {
    final static class DoubleStatistics extends DoubleSummaryStatistics {

        public static Collector<Double, ?, DoubleStatistics> collector() {
            return Collector.of(DoubleStatistics::new, DoubleStatistics::accept, DoubleStatistics::combine);
        }
        private double sumOfSquare = 0.0d;
        private double sumOfSquareCompensation;

        private double simpleSumOfSquare;

        @Override
        public void accept(double value) {
            super.accept(value);
            double squareValue = value * value;
            simpleSumOfSquare += squareValue;
            sumOfSquareWithCompensation(squareValue);
        }

        public DoubleStatistics combine(DoubleStatistics other) {
            super.combine(other);
            simpleSumOfSquare += other.simpleSumOfSquare;
            sumOfSquareWithCompensation(other.sumOfSquare);
            sumOfSquareWithCompensation(other.sumOfSquareCompensation);
            return this;
        }

        public final double getStandardDeviation() {
            long count = getCount();
            double sumOfSquare = getSumOfSquare();
            double average = getAverage();
            return count > 0 ? Math.sqrt((sumOfSquare - count * Math.pow(average, 2)) / (count - 1)) : 0.0d;
        }

        public double getSumOfSquare() {
            double tmp = sumOfSquare + sumOfSquareCompensation;
            if (Double.isNaN(tmp) && Double.isInfinite(simpleSumOfSquare)) {
                return simpleSumOfSquare;
            }
            return tmp;
        }

        private void sumOfSquareWithCompensation(double value) {
            double tmp = value - sumOfSquareCompensation;
            double velvel = sumOfSquare + tmp;
            sumOfSquareCompensation = (velvel - sumOfSquare) - tmp;
            sumOfSquare = velvel;
        }

    }
    private static final String JDBC_QUERY_RUN = "jdbc.queryRun";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String JDBC_JAR_PATH = "jdbc.jarPath";
    private static final String JDBC_MAX_ROWS = "jdbc.maxRows";
    private static final String JDBC_ROW_INDEX = "jdbc.rowIndex";
    private static final String JDBC_FETCH_SIZE = "jdbc.fetchSize";
    private static final String JDBC_PARAMETERS = "jdbc.parameters";
    private static final String JDBC_QUERY_FILE = "jdbc.queryFile";
    private static final String JDBC_QUERY = "jdbc.query";
    private static final String JDBC_PASSWORD = "jdbc.password";
    private static final String JDBC_USERNAME = "jdbc.username";
    private static final String JDBC_URL = "jdbc.url";
    private static final String JDBC_DRIVER = "jdbc.driver";
    private final static int MIN_EXECUTION_COUNT = 3;
    private final static int MAX_EXECUTION_COUNT = 10;

    private final static Logger LOGGER = Logger.getLogger("db-perfinder");

    static String getFormattedDataTable(List<Map<String, Object>> result) {
        if (result == null || result.isEmpty())
            return "";
        Map<String, Integer> headerLengths = new LinkedHashMap<>();
        Map<String, Boolean> headerTypes = new LinkedHashMap<>();
        result.parallelStream().findFirst()
                .ifPresent(p -> p.entrySet().forEach(e -> headerLengths.put(e.getKey(), e.getKey().length())));
        result.parallelStream().findFirst()
                .ifPresent(p -> p.entrySet().forEach(e -> headerTypes.put(e.getKey(), e.getValue() instanceof Number)));

        result.parallelStream()
                .forEach(p -> p.entrySet().stream().filter(e -> e.getValue() != null)
                        .filter(e -> e.getValue().toString().length() > headerLengths.get(e.getKey()))
                        .forEach(e -> headerLengths.put(e.getKey(), e.getValue().toString().length())));

        StringBuilder format = new StringBuilder();
        int[] counter = { 1 };
        headerLengths.entrySet().forEach(entry -> {
            format.append("|%" + (counter[0]++) + "$" + (headerTypes.get(entry.getKey()) ? "" : "-") + entry.getValue()
                    + "s");
        });

        format.append("|").append(LINE_SEPARATOR);
        String formatRows = format.toString();
        StringBuilder horizontalLines = new StringBuilder();
        IntStream.range(0, headerLengths.values().parallelStream().reduce(0, Integer::sum) + headerLengths.size())
                .parallel().forEach(i -> {
                    horizontalLines.append("-");
                });
        horizontalLines.append("-").append(LINE_SEPARATOR);

        StringBuilder rows = new StringBuilder();
        rows.append(horizontalLines);
        rows.append(String.format(formatRows, headerLengths.keySet().toArray(new Object[] {})));
        rows.append(horizontalLines);
        result.stream().forEach(p -> rows.append(String.format(formatRows, p.values().toArray(new Object[] {}))));
        rows.append(horizontalLines);

        return rows.toString();
    }

    private static Integer getIntegerValue(String property) {
        if (property == null || property.length() == 0)
            return null;
        return Integer.valueOf(property.trim());
    }

    private static String getPasswordFromCommand() {
        Console console = System.console();
        if (console == null) {
            throw new IllegalArgumentException("jdbc.password is not found in input or configuration!");
        }
        char[] passwordArray = console.readPassword("Enter jdbc.password: ");
        return new String(passwordArray);
    }

    public static void main(String[] argv) {
        try {

            String propertiesFile = System.getProperty("jdbc.properties");

            String logConfig = System.getProperty("java.util.logging.config.file");
            if (logConfig == null) {
                LOGGER.setUseParentHandlers(false);
                ConsoleHandler consoleHandler = new ConsoleHandler();

                consoleHandler.setFormatter(new Formatter() {

                    @Override
                    public String format(LogRecord record) {
                        return record.getLevel() + ":" + record.getMessage() + LINE_SEPARATOR;
                    }
                });

                LOGGER.addHandler(consoleHandler);
            }

            Properties jdbcProperties = new Properties();
            if (propertiesFile != null) {
                try (FileInputStream in = new FileInputStream(propertiesFile);) {
                    jdbcProperties.load(in);
                }
            } else {
                throw new FileNotFoundException("-Djdbc.properties=<JDBC Properties file> not set!");
            }
            LOGGER.fine(() -> "JDBC Properties:" + jdbcProperties);
            String jdbcDriverClassName = jdbcProperties.getProperty(JDBC_DRIVER);
            String jarPath = jdbcProperties.getProperty(JDBC_JAR_PATH);

            String connectionUrl = jdbcProperties.getProperty(JDBC_URL);
            String username = jdbcProperties.getProperty(JDBC_USERNAME);
            String password = jdbcProperties.getProperty(JDBC_PASSWORD);
            if ((username != null && username.length() > 0) && (password == null || password.length() == 0)) {
                password = getPasswordFromCommand();
            }
            String query = jdbcProperties.getProperty(JDBC_QUERY);
            if (query == null || query.trim().length() == 0) {
                String p = jdbcProperties.getProperty(JDBC_QUERY_FILE);
                if (p == null)
                    throw new FileNotFoundException("jdbc.query or jdbc.queryFile not found!");
                Path path = Paths.get(p);
                StringBuilder data = new StringBuilder();
                try (Stream<String> lines = Files.lines(path);) {
                    lines.forEach(line -> data.append(line).append(LINE_SEPARATOR));
                    query = data.toString();
                }
            }

            Integer runCount = Optional.ofNullable(getIntegerValue(jdbcProperties.getProperty(JDBC_QUERY_RUN)))
                    .orElse(MIN_EXECUTION_COUNT);
            runCount = runCount > MAX_EXECUTION_COUNT ? MAX_EXECUTION_COUNT : runCount;
            String[] parameters = Optional.ofNullable(jdbcProperties.getProperty(JDBC_PARAMETERS))
                    .map(s -> s.split(",")).orElse(null);
            Integer fetchSize = getIntegerValue(jdbcProperties.getProperty(JDBC_FETCH_SIZE));
            Integer rowIndex = getIntegerValue(jdbcProperties.getProperty(JDBC_ROW_INDEX));
            Integer maxRows = getIntegerValue(jdbcProperties.getProperty(JDBC_MAX_ROWS));

            try (RdbmsPerfinder rdbmsPf = new RdbmsPerfinder();) {
                LOGGER.fine(() -> "Loading JDBC Driver: " + jdbcDriverClassName);
                rdbmsPf.loadDriver(jdbcDriverClassName, jarPath);
                System.out.println("Creating Connection to: " + connectionUrl);
                rdbmsPf.createConnection(connectionUrl, username, password);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        LOGGER.info("Closing Connection and Exiting Gracefully!");
                        rdbmsPf.close();
                    } catch (Exception e) {
                        // nothing can be done
                    }
                }));
                LOGGER.info("Executing Query: " + query.toString());
                rdbmsPf.prepareStatement(query, fetchSize, rowIndex + maxRows, parameters);
                rdbmsPf.executeQuery(); // warm up
                List<Map<String, Object>> result = rdbmsPf.fetchResult(rowIndex); // dry
                                                                                  // run
                LOGGER.info("Query Executed Successfully!");
                LOGGER.info(() -> "Record Count: " + result.size());
                LOGGER.fine(() -> "Output: ");
                LOGGER.fine(() -> LINE_SEPARATOR + getFormattedDataTable(result));
                long[] queryTimes = new long[runCount];
                long[] fetchTimes = new long[runCount];
                LOGGER.info("Executing Tests...");
                for (int i = 0; i < runCount; i++) {
                    long time = System.currentTimeMillis();
                    rdbmsPf.executeQuery();
                    queryTimes[i] = System.currentTimeMillis() - time;
                    time = System.currentTimeMillis();
                    rdbmsPf.fetchResult(rowIndex);
                    fetchTimes[i] = System.currentTimeMillis() - time;
                    LOGGER.info("Executing Test Run: " + (i + 1));

                }
                LOGGER.info("Test Result...");

                LOGGER.info("Query Times (ms): All" + Arrays.toString(queryTimes) + ", Avg["
                        + new BigDecimal(Arrays.stream(queryTimes).average().getAsDouble()).setScale(0,
                                BigDecimal.ROUND_HALF_UP)
                        + "], Stdiv["
                        + new BigDecimal(Arrays.stream(queryTimes).mapToDouble(r -> new Double(r)).boxed()
                                .collect(DoubleStatistics.collector()).getStandardDeviation()).setScale(1,
                                        BigDecimal.ROUND_HALF_UP)
                        + "]");
                LOGGER.info("Fetch Times (ms): All" + Arrays.toString(fetchTimes) + ", Avg["
                        + new BigDecimal(Arrays.stream(fetchTimes).average().getAsDouble()).setScale(0,
                                BigDecimal.ROUND_HALF_UP)
                        + "], Stdiv["
                        + new BigDecimal(Arrays.stream(fetchTimes).mapToDouble(r -> new Double(r)).boxed()
                                .collect(DoubleStatistics.collector()).getStandardDeviation()).setScale(1,
                                        BigDecimal.ROUND_HALF_UP)
                        + "]");
            }

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LOGGER.severe(() -> "System Exception:" + sw.toString());
        }
    }
}
