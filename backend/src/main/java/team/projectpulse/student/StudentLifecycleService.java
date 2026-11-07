package team.projectpulse.student;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import team.projectpulse.system.EmailService;

/**
 * Orchestrates a change to a student's account lifecycle: the state change, then the email that tells her.
 *
 * <p><strong>Deliberately not transactional.</strong> {@link StudentService#deactivateStudent} runs in its own
 * transaction, which commits when it returns, and only then is the email sent. So no row lock is held during SMTP,
 * an SMTP failure cannot roll the deactivation back, and no email describes a deactivation that did not commit
 * (docs/design/stu.md, D3). A failed send is reported to the caller instead (extension 6b).
 */
@Service
public class StudentLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(StudentLifecycleService.class);

    private final StudentService studentService;
    private final EmailService emailService;


    public StudentLifecycleService(StudentService studentService, EmailService emailService) {
        this.studentService = studentService;
        this.emailService = emailService;
    }

    public DeactivationOutcome deactivateStudent(Integer studentId) {
        DeactivationResult result = this.studentService.deactivateStudent(studentId);
        if (result.alreadyDeactivated()) {
            return new DeactivationOutcome(result.student(), true, false); // 6a: no change, so no email
        }

        boolean emailSent;
        try {
            this.emailService.sendDeactivationEmail(result.student());
            emailSent = true;
        } catch (RuntimeException e) {
            log.warn("Student {} was deactivated, but the deactivation email could not be sent", studentId, e);
            emailSent = false; // 6b: she stays deactivated
        }
        return new DeactivationOutcome(result.student(), false, emailSent);
    }

}
