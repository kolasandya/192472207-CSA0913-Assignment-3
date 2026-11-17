# CSA09 - Programming in Java | Assignment 3 (CO3)

- **Student Name:** <Your Name>
- **Register Number:** <Your Register Number>
- **Java Version:** Java 17+ (compatible with Java 11+)
- **IDE:** Visual Studio Code (Extension Pack for Java)

## Folder Structure
Each question has its own standalone folder with source code, sample input files, and instructions:
- `Q1_BankATM/` – Exception handling (`try`, `catch`, multi-catch, `finally`) & ATM simulation
- `Q2_ExamPortal/` – User-defined checked/unchecked exceptions, propagation & chaining + `StorageDemo`
- `Q3_DailySalesReport/` – Resource safety, `try-catch-finally` vs `try-with-resources` & leak debugging
- `Q4_ParallelScanning/` – `Thread` vs `Runnable`, thread lifecycle, `join()`, priorities & daemon threads
- `Q5_CinemaSeatBooking/` – Race conditions, synchronized methods/blocks & `SeatBooking`
- `Q6_DeadlockAndTokenCounter/` – Deadlock reproduction/prevention & Producer-Consumer (`TokenCounter`)
- `Q7_SemesterResultProcessor/` – Concurrent fault-tolerant processor (`ExecutorService`, `Callable`, `Future`)

## How to Run in VS Code
1. Extract the ZIP archive and open the `CSA09-Assignment3` folder in **VS Code**.
2. Install the **Extension Pack for Java** (`vscjava.vscode-java-pack`) if not already installed.
3. Open any `.java` file containing a `main` method and click **Run** above `public static void main`, or open the integrated terminal (`Ctrl + ~`) and run the `javac` / `java` commands listed in each task's folder.

## Summary of Results
- **Q1:** Validated multi-catch (`NumberFormatException | InputMismatchException`), checked `FileNotFoundException`, and custom `InsufficientBalanceException` / `InvalidAmountException`, confirming `finally` executes across all paths.
- **Q2:** Implemented layered exception propagation (`Storage` -> `Service` -> `Controller`) with `initCause`/constructor chaining and verified `StorageDemo` output.
- **Q3:** Demonstrated file handle leak in manual `try-catch-finally` upon mid-stream `NumberFormatException`, and verified zero leaks + suppressed exception handling in `try-with-resources`.
- **Q4:** Verified non-deterministic thread scheduling across 10 runs, `join()` barrier synchronization, priority hints, and daemon termination.
- **Q5:** Reproduced double-booking race condition in unsynchronized `SeatBooking` and eliminated it using `synchronized` methods and fine-grained blocks.
- **Q6:** Reproduced circular-wait deadlock in wallet transfers, resolved it via consistent ID lock ordering, and verified `TokenCounter` sum = `210`.
- **Q7:** Processed 10,000 score files concurrently with `ExecutorService`, isolating corrupt/missing file failures and benchmarking 1, 2, 4, and 8 threads.
