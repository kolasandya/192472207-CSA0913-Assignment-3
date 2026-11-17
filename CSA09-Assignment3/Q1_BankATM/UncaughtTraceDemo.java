package Q1_BankATM;

// Helper demo for Student B: Generates an uncaught exception stack trace & shows catch-order rules
public class UncaughtTraceDemo {
    public static void main(String[] args) {
        // Deliberately triggers an uncaught ArithmeticException / NumberFormatException
        // to inspect the full JVM stack trace line by line.
        String badInput = "FIFTY_DOLLARS";
        int amount = Integer.parseInt(badInput); // Throws uncaught NumberFormatException
        System.out.println("Dispensing: " + amount);
    }
}
