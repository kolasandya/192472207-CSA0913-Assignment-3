package Q4_ParallelScanning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Approach 1: Extending Thread
class ScannerStationThread extends Thread {
    private final int scriptsToScan;
    private final AtomicInteger totalScanned;
    private final List<String> completionOrder;

    public ScannerStationThread(String name, int scriptsToScan,
                                AtomicInteger totalScanned, List<String> completionOrder) {
        super(name);
        this.scriptsToScan = scriptsToScan;
        this.totalScanned = totalScanned;
        this.completionOrder = completionOrder;
    }

    @Override
    public void run() {
        System.out.println("[" + getName() + "] State inside run(): " + getState()
                + " | Executing on OS thread: " + Thread.currentThread().getName());
        for (int i = 1; i <= scriptsToScan; i++) {
            try {
                // TIMED_WAITING state during sleep()
                Thread.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            totalScanned.incrementAndGet();
        }
        completionOrder.add(getName());
        System.out.println("[" + getName() + "] Finished scanning " + scriptsToScan + " scripts.");
    }
}

// Approach 2: Implementing Runnable (Preferred for decoupling task from thread worker)
class ScannerStationTask implements Runnable {
    private final String stationName;
    private final int scriptsToScan;
    private final AtomicInteger totalScanned;
    private final List<String> completionOrder;

    public ScannerStationTask(String stationName, int scriptsToScan,
                              AtomicInteger totalScanned, List<String> completionOrder) {
        this.stationName = stationName;
        this.scriptsToScan = scriptsToScan;
        this.totalScanned = totalScanned;
        this.completionOrder = completionOrder;
    }

    @Override
    public void run() {
        for (int i = 1; i <= scriptsToScan; i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            totalScanned.incrementAndGet();
        }
        completionOrder.add(stationName);
    }
}

public class ParallelScriptScanning {

    public static void runSingleTrial(int runNumber, boolean usePriorities) throws InterruptedException {
        AtomicInteger totalScanned = new AtomicInteger(0);
        List<String> completionOrder = Collections.synchronizedList(new ArrayList<>());

        Thread[] stations = new Thread[4];
        for (int i = 0; i < 4; i++) {
            stations[i] = new Thread(
                new ScannerStationTask("Station-" + (i + 1), 10, totalScanned, completionOrder),
                "Station-" + (i + 1)
            );
        }

        if (usePriorities) {
            stations[0].setPriority(Thread.MIN_PRIORITY);  // 1
            stations[3].setPriority(Thread.MAX_PRIORITY);  // 10
        }

        for (Thread t : stations) t.start();
        for (Thread t : stations) t.join();

        System.out.println("Run #" + runNumber + " finish order: " + completionOrder
                + " | Total Scanned: " + totalScanned.get());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Part 1: Thread Life Cycle States & Extending Thread ===");
        AtomicInteger total = new AtomicInteger(0);
        List<String> order = Collections.synchronizedList(new ArrayList<>());

        ScannerStationThread s1 = new ScannerStationThread("Station-A", 5, total, order);
        System.out.println("Before start() -> State: " + s1.getState() + ", isAlive: " + s1.isAlive());

        s1.start();
        System.out.println("Right after start() -> State: " + s1.getState() + ", isAlive: " + s1.isAlive());

        s1.join();
        System.out.println("After join() -> State: " + s1.getState() + ", isAlive: " + s1.isAlive());

        System.out.println("
=== Part 2: Calling run() Directly vs start() ===");
        ScannerStationThread directCall = new ScannerStationThread("DirectRunStation", 2, total, order);
        // Calling run() executes synchronously on 'main' thread instead of spawning a new thread!
        directCall.run();

        System.out.println("
=== Part 3: 10 Runs with Runnable (Observing Non-Determinism) ===");
        for (int r = 1; r <= 10; r++) {
            runSingleTrial(r, false);
        }

        System.out.println("
=== Part 4: Priority Experiment (Station-1 MIN, Station-4 MAX) ===");
        for (int r = 1; r <= 3; r++) {
            runSingleTrial(r, true);
        }

        System.out.println("
=== Part 5: Daemon Station Experiment ===");
        Thread daemonStation = new Thread(() -> {
            try {
                System.out.println("[Daemon-Station] Started background continuous scan...");
                Thread.sleep(5000); // Will be abruptly terminated when main() exits!
                System.out.println("[Daemon-Station] Completed (will NOT print if main exits early).");
            } catch (InterruptedException ignored) {}
        });
        daemonStation.setDaemon(true);
        daemonStation.start();
        Thread.sleep(100);
        System.out.println("Main thread ending now; JVM terminates Daemon-Station immediately.");
    }
}
