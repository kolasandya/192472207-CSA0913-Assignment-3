package Q6_DeadlockAndTokenCounter;

public class HostelWalletDeadlock {

    static class Wallet {
        final int id;
        double balance;

        Wallet(int id, double balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    // Version A: Deadlock-prone transfer (locks 'from' then 'to' in arbitrary caller order)
    public static void transferDeadlockProne(Wallet from, Wallet to, double amount) {
        synchronized (from) {
            System.out.println(Thread.currentThread().getName() + " locked Wallet-" + from.id);
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            synchronized (to) {
                from.balance -= amount;
                to.balance += amount;
                System.out.println("Transferred " + amount + " from Wallet-" + from.id + " to Wallet-" + to.id);
            }
        }
    }

    // Version B: Deadlock-free transfer using consistent global lock ordering by Wallet ID
    public static void transferDeadlockFree(Wallet from, Wallet to, double amount) {
        Wallet first = (from.id < to.id) ? from : to;
        Wallet second = (from.id < to.id) ? to : from;

        synchronized (first) {
            try { Thread.sleep(20); } catch (InterruptedException ignored) {}
            synchronized (second) {
                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                    System.out.printf("[%s] Safely transferred %.2f (Wallet-%d -> Wallet-%d)%n",
                            Thread.currentThread().getName(), amount, from.id, to.id);
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Wallet w1 = new Wallet(101, 1000.0);
        Wallet w2 = new Wallet(102, 1000.0);

        System.out.println("=== 1. Deadlock-Free Transfers (Consistent Lock Ordering) ===");
        Thread t1 = new Thread(() -> transferDeadlockFree(w1, w2, 200.0), "Student-A");
        Thread t2 = new Thread(() -> transferDeadlockFree(w2, w1, 150.0), "Student-B");
        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.printf("Final Balances -> Wallet-101: %.2f | Wallet-102: %.2f%n", w1.balance, w2.balance);

        System.out.println("
=== 2. Deadlock Demonstration (Detected after 500ms timeout) ===");
        Thread d1 = new Thread(() -> transferDeadlockProne(w1, w2, 100.0), "Deadlock-T1");
        Thread d2 = new Thread(() -> transferDeadlockProne(w2, w1, 100.0), "Deadlock-T2");
        d1.start(); d2.start();
        d1.join(500);
        d2.join(500);
        if (d1.isAlive() && d2.isAlive()) {
            System.out.println("DEADLOCK DETECTED: Both threads are blocked in state " + d1.getState() + "!");
            // Exit explicitly since deadlocked non-daemon threads will never finish
            System.exit(0);
        }
    }
}
