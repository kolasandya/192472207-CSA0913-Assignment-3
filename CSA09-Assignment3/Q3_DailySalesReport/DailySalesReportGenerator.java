package Q3_DailySalesReport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DailySalesReportGenerator {
    private static final Logger LOGGER = Logger.getLogger(DailySalesReportGenerator.class.getName());

    // Helper to resolve paths whether run from workspace root or Q3 folder
    private static String resolvePath(String relPath) {
        File f = new File("Q3_DailySalesReport/" + relPath);
        return f.exists() ? f.getPath() : relPath;
    }

    /**
     * VERSION 1 (Buggy Manual try-catch-finally):
     * Demonstrates why manual resource management leaks open handles when an exception occurs
     * before close() is reached (or when multiple readers are opened in a loop).
     */
    public static int generateReportManualLeaky(List<String> storeFiles, String outputFile) {
        List<BufferedReader> leakedReaders = new ArrayList<>();
        BufferedWriter writer = null;
        double grandTotal = 0.0;

        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
            for (String file : storeFiles) {
                // Opens a new reader; if parsing throws NumberFormatException, loop aborts
                // and all readers opened so far remain UNCLOSED!
                BufferedReader reader = new BufferedReader(new FileReader(file));
                leakedReaders.add(reader);

                String line;
                double storeTotal = 0.0;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    // Breakpoint target: fails on store2.txt ("INVALID_ENTRY")
                    storeTotal += Double.parseDouble(line.trim());
                }
                writer.write(file + " Total: " + storeTotal + "
");
                grandTotal += storeTotal;
                // Notice: reader.close() was forgotten inside the loop!
            }
            writer.write("GRAND TOTAL: " + grandTotal + "
");
        } catch (IOException | NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Manual version failed while processing files: " + e.getMessage());
        } finally {
            // Only closes writer; all BufferedReaders in leakedReaders remain open!
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to close writer", e);
                }
            }
        }
        return leakedReaders.size(); // Number of unclosed file handles leaked
    }

    /**
     * VERSION 2 (Safe try-with-resources):
     * Guarantees every BufferedReader and BufferedWriter is closed automatically
     * in reverse order of declaration, even if NumberFormatException or IOException occurs.
     */
    public static int generateReportTryWithResources(List<String> storeFiles, String outputFile) {
        int openHandlesAtExit = 0;
        double grandTotal = 0.0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String file : storeFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    double storeTotal = 0.0;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        storeTotal += Double.parseDouble(line.trim());
                    }
                    writer.write(file + " Total: " + String.format("%.2f", storeTotal) + "
");
                    grandTotal += storeTotal;
                } catch (NumberFormatException | IOException fileEx) {
                    // Logging exact file that failed; reader is ALREADY closed by try-with-resources
                    LOGGER.log(Level.WARNING, "Error in file [" + file + "]: " + fileEx.getMessage());
                    throw fileEx; // Re-throw to simulate batch abort while still closing writer safely
                }
            }
            writer.write("GRAND TOTAL: " + String.format("%.2f", grandTotal) + "
");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Try-With-Resources aborted safely. Leaked handles: " + openHandlesAtExit);
        }
        return openHandlesAtExit;
    }

    public static void main(String[] args) {
        List<String> validFiles = List.of(
            resolvePath("store1.txt"),
            resolvePath("store3.txt")
        );

        List<String> failingFiles = List.of(
            resolvePath("store1.txt"),
            resolvePath("store2.txt"), // Contains INVALID_ENTRY
            resolvePath("store3.txt")
        );

        String outManual = new File("Q3_DailySalesReport").exists()
                ? "Q3_DailySalesReport/report_manual.txt" : "report_manual.txt";
        String outTwr = new File("Q3_DailySalesReport").exists()
                ? "Q3_DailySalesReport/report_twr.txt" : "report_twr.txt";

        System.out.println("=== 1. Valid Input Run ===");
        generateReportTryWithResources(validFiles, outTwr);
        System.out.println("Valid report written to: " + outTwr);

        System.out.println("
=== 2. Failing Input Run (store2.txt has corrupt number) ===");
        int leakedManual = generateReportManualLeaky(failingFiles, outManual);
        int leakedTwr = generateReportTryWithResources(failingFiles, outTwr);

        System.out.println("Files left open in Manual version: " + leakedManual);
        System.out.println("Files left open in Try-With-Resources version: " + leakedTwr);
    }
}
