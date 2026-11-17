package Q1_BankATM;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;

// Custom Checked Exception for business rule violation
class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

// Custom Unchecked Exception for invalid argument/input state
class InvalidAmountException extends IllegalArgumentException {
    public InvalidAmountException(String message) {
        super(message);
    }
}

public class BankATMSimulation {

    // Reads account balances from card-reader file (Checked: FileNotFoundException, IOException)
    private static Map<String, Double> loadCardReaderData(String filePath) throws IOException {
        Map<String, Double> accounts = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                accounts.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
            }
        }
        return accounts;
    }

    public static void performWithdrawal(String cardFilePath, String accountNumber, String rawAmount)
            throws FileNotFoundException, IOException, InsufficientBalanceException {

        // 1. Checked Exception point: missing or unreadable card-reader file
        Map<String, Double> accounts = loadCardReaderData(cardFilePath);

        if (!accounts.containsKey(accountNumber)) {
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }

        // 2. Unchecked Exception point: user types letters instead of numbers (NumberFormatException)
        double amount = Double.parseDouble(rawAmount);

        // 3. Unchecked Exception point: negative or zero withdrawal amount
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero. Entered: " + amount);
        }

        double currentBalance = accounts.get(accountNumber);

        // 4. Checked Exception point: requesting more than available balance
        if (amount > currentBalance) {
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Available: %.2f, Requested: %.2f", currentBalance, amount)
            );
        }

        // Dispense cash and update balance
        double remaining = currentBalance - amount;
        accounts.put(accountNumber, remaining);
        System.out.printf("SUCCESS: Dispensed Rs. %.2f from %s. Remaining Balance: Rs. %.2f%n",
                amount, accountNumber, remaining);
    }

    public static void runTestCase(String testName, String filePath, String accNo, String amountInput) {
        System.out.println("--------------------------------------------------");
        System.out.println("Test Case: " + testName);
        boolean sessionActive = true;
        try {
            performWithdrawal(filePath, accNo, amountInput);
        } catch (FileNotFoundException e) {
            // Narrower checked I/O exception placed BEFORE IOException
            System.out.println("[Caught FileNotFoundException - Checked]: Card-reader file missing -> " + e.getMessage());
        } catch (InsufficientBalanceException e) {
            // Domain checked exception
            System.out.println("[Caught InsufficientBalanceException - Checked]: " + e.getMessage());
        } catch (NumberFormatException | InputMismatchException e) {
            // Multi-catch for invalid numeric input (Unchecked)
            System.out.println("[Caught Multi-Catch NumberFormat/InputMismatch - Unchecked]: Non-numeric amount -> " + e.getMessage());
        } catch (InvalidAmountException e) {
            // Narrower unchecked exception placed BEFORE IllegalArgumentException
            System.out.println("[Caught InvalidAmountException - Unchecked]: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Broader unchecked argument exception
            System.out.println("[Caught IllegalArgumentException - Unchecked]: " + e.getMessage());
        } catch (IOException e) {
            // Broader checked I/O exception
            System.out.println("[Caught IOException - Checked]: General I/O failure -> " + e.getMessage());
        } finally {
            // Guaranteed cleanup: ejects card and closes ATM session regardless of outcome
            sessionActive = false;
            System.out.println("[FINALLY]: Card ejected and ATM session closed (sessionActive=" + sessionActive + ").");
        }
    }

    public static void main(String[] args) {
        String validFile = "Q1_BankATM/card_reader.txt";
        // Fallback if executed directly from inside Q1_BankATM directory
        if (!new java.io.File(validFile).exists()) {
            validFile = "card_reader.txt";
        }

        // 5+ distinct test paths for Student B tracing
        runTestCase("1. Valid Withdrawal", validFile, "ACC1001", "2500");
        runTestCase("2. Non-numeric Amount (Letters)", validFile, "ACC1001", "abc");
        runTestCase("3. Negative Amount", validFile, "ACC1002", "-500");
        runTestCase("4. Insufficient Balance", validFile, "ACC1003", "1000");
        runTestCase("5. Missing Card-Reader File", "missing_card_reader.txt", "ACC1001", "500");
        runTestCase("6. Unknown Account Number", validFile, "ACC9999", "500");

        // Optional interactive mode
        if (args.length > 0 && args[0].equalsIgnoreCase("--interactive")) {
            Scanner sc = new Scanner(System.in);
            System.out.print("
Enter Account Number: ");
            String acc = sc.nextLine();
            System.out.print("Enter Withdrawal Amount: ");
            String amt = sc.nextLine();
            runTestCase("Interactive Withdrawal", validFile, acc, amt);
            sc.close();
        }
    }
}
