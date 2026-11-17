package Q7_SemesterResultProcessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class SemesterResultProcessor {

    // Immutable result record for each student
    static class StudentResult {
        final String studentId;
        final int totalMarks;
        final double percentage;
        final String grade;

        StudentResult(String studentId, int totalMarks, double percentage, String grade) {
            this.studentId = studentId;
            this.totalMarks = totalMarks;
            this.percentage = percentage;
            this.grade = grade;
        }
    }

    // Callable task that processes one file independently and isolates failures
    static class ScoreFileTask implements Callable<Boolean> {
        private final File file;
        private final ConcurrentHashMap<String, StudentResult> resultsMap;
        private final ConcurrentHashMap<String, String> errorLog;
        private final AtomicInteger successCount;
        private final AtomicInteger failureCount;

        ScoreFileTask(File file,
                      ConcurrentHashMap<String, StudentResult> resultsMap,
                      ConcurrentHashMap<String, String> errorLog,
                      AtomicInteger successCount,
                      AtomicInteger failureCount) {
            this.file = file;
            this.resultsMap = resultsMap;
            this.errorLog = errorLog;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        @Override
        public Boolean call() {
            try {
                if (!file.exists()) {
                    throw new IOException("Missing score file: " + file.getName());
                }
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line = br.readLine();
                    if (line == null || line.trim().isEmpty()) {
                        throw new IllegalArgumentException("Empty file: " + file.getName());
                    }
                    // Expected format: REG10001,85,90,78,88,92
                    String[] parts = line.trim().split(",");
                    if (parts.length != 6) {
                        throw new IllegalArgumentException("Malformed record in " + file.getName());
                    }
                    String studentId = parts[0];
                    int sum = 0;
                    for (int i = 1; i <= 5; i++) {
                        int mark = Integer.parseInt(parts[i].trim());
                        if (mark < 0 || mark > 100) {
                            throw new IllegalArgumentException("Out-of-range mark (" + mark + ") in " + file.getName());
                        }
                        sum += mark;
                    }
                    double pct = sum / 5.0;
                    String grade = (pct >= 90) ? "S" : (pct >= 80) ? "A" : (pct >= 70) ? "B" : (pct >= 50) ? "C" : "F";
                    resultsMap.put(studentId, new StudentResult(studentId, sum, pct, grade));
                    successCount.incrementAndGet();
                    return true;
                }
            } catch (Exception e) {
                // Fault isolation: record failure without crashing pool or other tasks
                errorLog.put(file.getName(), e.getClass().getSimpleName() + ": " + e.getMessage());
                failureCount.incrementAndGet();
                return false;
            }
        }
    }

    // Generates 10,000 test files (with injected missing and corrupt files)
    private static List<File> generateTestDataset(Path dir, int totalFiles) throws IOException {
        Files.createDirectories(dir);
        List<File> fileList = new ArrayList<>(totalFiles);
        for (int i = 1; i <= totalFiles; i++) {
            File f = dir.resolve(String.format("score_%05d.txt", i)).toFile();
            fileList.add(f);

            if (i % 500 == 0) {
                // Inject MISSING file (do not create on disk; delete if leftover)
                if (f.exists()) f.delete();
            } else if (i % 333 == 0) {
                // Inject CORRUPT file (non-numeric marks)
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
                    bw.write("REG" + i + ",80,CORRUPT_MARK,75,90,85
");
                }
            } else {
                // Valid file
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
                    bw.write("REG" + i + ",82,76,91,88,79
");
                }
            }
        }
        return fileList;
    }

    public static void runBatchWithPoolSize(List<File> files, int poolSize) throws InterruptedException {
        ConcurrentHashMap<String, StudentResult> resultsMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> errorLog = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        List<Future<Boolean>> futures = new ArrayList<>(files.size());

        long startNs = System.nanoTime();
        for (File f : files) {
            futures.add(executor.submit(new ScoreFileTask(f, resultsMap, errorLog, successCount, failureCount)));
        }

        // Wait for every Future to complete
        for (Future<Boolean> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                // Already handled inside Callable, kept as defensive barrier
            }
        }
        executor.shutdown();
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        System.out.printf("Pool Size: %d | Total Files: %d | Succeeded: %d | Failed: %d | Time: %d ms%n",
                poolSize, files.size(), successCount.get(), failureCount.get(), elapsedMs);
    }

    public static void main(String[] args) throws Exception {
        Path datasetDir = new File("Q7_SemesterResultProcessor").exists()
                ? Path.of("Q7_SemesterResultProcessor/sample_scores")
                : Path.of("sample_scores");

        int totalFiles = 10_000;
        System.out.println("Generating " + totalFiles + " score files (with injected missing & corrupt files)...");
        List<File> files = generateTestDataset(datasetDir, totalFiles);

        System.out.println("
=== Benchmarking Thread Pool Sizes (1, 2, 4, 8) ===");
        for (int poolSize : new int[]{1, 2, 4, 8}) {
            runBatchWithPoolSize(files, poolSize);
        }
    }
}
