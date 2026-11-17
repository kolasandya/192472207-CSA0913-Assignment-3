package Q2_ExamPortal;

import java.io.IOException;
import java.util.Set;

// Checked Exceptions: Recoverable business/storage rules caller must handle
class ExamWindowClosedException extends ExamException {
    public ExamWindowClosedException(String message) {
        super(message);
    }
}

class StudentNotRegisteredException extends ExamException {
    public StudentNotRegisteredException(String message) {
        super(message);
    }
}

// Unchecked Exception: Programming/contract bug (e.g., null payload)
class InvalidSubmissionPayloadException extends RuntimeException {
    public InvalidSubmissionPayloadException(String message) {
        super(message);
    }
}

// Layer 3: Storage Layer
class SubmissionStorageLayer {
    public void storeAnswerFile(String studentId, String fileName, long sizeKb)
            throws SubmissionStorageException {
        try {
            if (sizeKb > 500) {
                throw new IOException("File size " + sizeKb + "KB exceeds 500KB limit");
            }
            System.out.println("[Storage] Stored " + fileName + " for " + studentId);
        } catch (IOException ioEx) {
            // Exception chaining preserves low-level IOException
            throw new SubmissionStorageException("Failed to persist submission file: " + fileName, ioEx);
        }
    }
}

// Layer 2: Service Layer
class SubmissionServiceLayer {
    private final SubmissionStorageLayer storage = new SubmissionStorageLayer();
    private final Set<String> registeredStudents = Set.of("REG101", "REG102");

    public void processSubmission(String studentId, String courseCode, boolean windowOpen,
                                  String fileName, long sizeKb) throws ExamException {
        if (studentId == null || fileName == null) {
            throw new InvalidSubmissionPayloadException("Student ID and file name must not be null.");
        }
        if (!windowOpen) {
            throw new ExamWindowClosedException("Exam window has closed for course " + courseCode);
        }
        if (!registeredStudents.contains(studentId)) {
            throw new StudentNotRegisteredException("Student " + studentId + " is not registered for " + courseCode);
        }
        // Propagates SubmissionStorageException upward
        storage.storeAnswerFile(studentId, fileName, sizeKb);
    }
}

// Layer 1: Controller Layer
public class ExamPortalController {
    private final SubmissionServiceLayer service = new SubmissionServiceLayer();

    public void handleSubmitRequest(String studentId, String courseCode, boolean windowOpen,
                                    String fileName, long sizeKb) {
        System.out.println("
--- Submission Request: " + studentId + " | " + fileName + " (" + sizeKb + "KB) ---");
        try {
            service.processSubmission(studentId, courseCode, windowOpen, fileName, sizeKb);
            System.out.println("[Controller HTTP 200] Submission accepted successfully.");
        } catch (ExamWindowClosedException e) {
            System.out.println("[Controller HTTP 403] Rejected: " + e.getMessage());
        } catch (StudentNotRegisteredException e) {
            System.out.println("[Controller HTTP 401] Rejected: " + e.getMessage());
        } catch (SubmissionStorageException e) {
            System.out.println("[Controller HTTP 500] Storage Failure: " + e.getMessage());
            System.out.println("   Root Cause via getCause(): " + e.getCause());
            e.printStackTrace(System.out);
        } catch (InvalidSubmissionPayloadException e) {
            System.out.println("[Controller HTTP 400] Bad Request: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ExamPortalController controller = new ExamPortalController();

        // Scenario 1: Valid submission
        controller.handleSubmitRequest("REG101", "CSA09", true, "reg101_ans.pdf", 250);

        // Scenario 2: Exam window closed
        controller.handleSubmitRequest("REG101", "CSA09", false, "reg101_late.pdf", 200);

        // Scenario 3: Unregistered student
        controller.handleSubmitRequest("REG999", "CSA09", true, "reg999_ans.pdf", 200);

        // Scenario 4: Oversized file -> Storage IOException chained to SubmissionStorageException
        controller.handleSubmitRequest("REG102", "CSA09", true, "reg102_huge.pdf", 1200);
    }
}
