package team.projectpulse.student;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.system.EmailService;
import team.projectpulse.system.exception.ObjectNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UC-STU-deactivate-student, the orchestration (docs/design/stu.md, D3): the email follows the committed state
 * change, 6a sends none, and 6b keeps the deactivation and reports the failed send.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentLifecycleService")
class StudentLifecycleServiceTest {

    @Mock
    StudentService studentService;

    @Mock
    EmailService emailService;

    @InjectMocks
    StudentLifecycleService studentLifecycleService;

    private final Student john = new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", false, "student");


    @Test
    void theEmailIsSentOnlyAfterTheStateChangeReturns() {
        given(this.studentService.deactivateStudent(4)).willReturn(new DeactivationResult(this.john, false));

        DeactivationOutcome outcome = this.studentLifecycleService.deactivateStudent(4);

        InOrder order = inOrder(this.studentService, this.emailService);
        order.verify(this.studentService).deactivateStudent(4);
        order.verify(this.emailService).sendDeactivationEmail(this.john);
        assertThat(outcome.alreadyDeactivated()).isFalse();
        assertThat(outcome.emailSent()).isTrue();
        assertThat(outcome.student()).isSameAs(this.john);
    }

    @Test
    void aFailedStateChangeSendsNoEmail() {
        given(this.studentService.deactivateStudent(4)).willThrow(new ObjectNotFoundException("student", 4));

        assertThatThrownBy(() -> this.studentLifecycleService.deactivateStudent(4)).isInstanceOf(ObjectNotFoundException.class);
        verify(this.emailService, never()).sendDeactivationEmail(any());
    }

    @Test
    void anAlreadyDeactivatedStudentIsNotEmailed() {
        // 6a
        given(this.studentService.deactivateStudent(4)).willReturn(new DeactivationResult(this.john, true));

        DeactivationOutcome outcome = this.studentLifecycleService.deactivateStudent(4);

        assertThat(outcome.alreadyDeactivated()).isTrue();
        assertThat(outcome.emailSent()).isFalse();
        verify(this.emailService, never()).sendDeactivationEmail(any());
    }

    @Test
    void aFailedSendKeepsTheDeactivationAndReportsIt() {
        // 6b
        given(this.studentService.deactivateStudent(4)).willReturn(new DeactivationResult(this.john, false));
        willThrow(new RuntimeException("SMTP down")).given(this.emailService).sendDeactivationEmail(this.john);

        DeactivationOutcome outcome = this.studentLifecycleService.deactivateStudent(4);

        assertThat(outcome.alreadyDeactivated()).isFalse();
        assertThat(outcome.emailSent()).isFalse();
        assertThat(outcome.student().isEnabled()).isFalse();
    }

}
