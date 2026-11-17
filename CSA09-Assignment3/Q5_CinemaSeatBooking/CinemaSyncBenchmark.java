package Q5_CinemaSeatBooking;

public class CinemaSyncBenchmark {

    // 1. Unsynchronized version (produces double bookings)
    static class UnsynchronizedCinema {
        private final boolean[] booked;
        private int bookingsMade = 0;

        UnsynchronizedCinema(int seats) { this.booked = new boolean[seats]; }

        public void book(int seat) {
            if (!booked[seat]) {
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
                booked[seat] = true;
                bookingsMade++;
            }
        }
    }

    // 2. Synchronized Method version (locks entire 'this' instance)
    static class SyncMethodCinema {
        private final boolean[] booked;
        private int bookingsMade = 0;

        SyncMethodCinema(int seats) { this.booked = new boolean[seats]; }

        public synchronized void book(int seat) {
            if (!booked[seat]) {
                booked[seat] = true;
                bookingsMade++;
            }
        }
    }

    // 3. Fine-Grained Synchronized Block version (locks only the critical section)
    static class SyncBlockCinema {
        private final boolean[] booked;
        private int bookingsMade = 0;
        private final Object seatLock = new Object();

        SyncBlockCinema(int seats) { this.booked = new boolean[seats]; }

        public void book(int seat) {
            // Non-critical pre-validation can run concurrently outside lock
            if (seat < 0 || seat >= booked.length) return;

            synchronized (seatLock) {
                if (!booked[seat]) {
                    booked[seat] = true;
                    bookingsMade++;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1. Reproducing Race Condition (10 Runs without Synchronization) ===");
        for (int run = 1; run <= 10; run++) {
            UnsynchronizedCinema cinema = new UnsynchronizedCinema(10);
            Thread[] users = new Thread[100];
            for (int i = 0; i < 100; i++) {
                final int seat = i % 10;
                users[i] = new Thread(() -> cinema.book(seat));
                users[i].start();
            }
            for (Thread t : users) t.join();
            System.out.printf("Run %2d -> Seats sold: %d (Double bookings: %d)%n",
                    run, cinema.bookingsMade, Math.max(0, cinema.bookingsMade - 10));
        }

        System.out.println("
=== 2. Benchmarking 10,000 Bookings (Sync Method vs Sync Block) ===");
        int totalRequests = 10_000;
        int totalSeats = 1000;

        SyncMethodCinema methodCinema = new SyncMethodCinema(totalSeats);
        long t1 = System.nanoTime();
        Thread[] mThreads = new Thread[totalRequests];
        for (int i = 0; i < totalRequests; i++) {
            final int seat = i % totalSeats;
            mThreads[i] = new Thread(() -> methodCinema.book(seat));
            mThreads[i].start();
        }
        for (Thread t : mThreads) t.join();
        long methodTimeMs = (System.nanoTime() - t1) / 1_000_000;

        SyncBlockCinema blockCinema = new SyncBlockCinema(totalSeats);
        long t2 = System.nanoTime();
        Thread[] bThreads = new Thread[totalRequests];
        for (int i = 0; i < totalRequests; i++) {
            final int seat = i % totalSeats;
            bThreads[i] = new Thread(() -> blockCinema.book(seat));
            bThreads[i].start();
        }
        for (Thread t : bThreads) t.join();
        long blockTimeMs = (System.nanoTime() - t2) / 1_000_000;

        System.out.println("Synchronized Method -> Sold: " + methodCinema.bookingsMade + " | Time: " + methodTimeMs + " ms");
        System.out.println("Synchronized Block  -> Sold: " + blockCinema.bookingsMade + " | Time: " + blockTimeMs + " ms");
    }
}
