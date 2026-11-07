package team.projectpulse.student.dto;

/**
 * The data of a {@code PATCH /students/{studentId}/deactivation} response: the student as she now is, whether she
 * was already deactivated (extension 6a), and whether the notification email was sent (extension 6b).
 */
public record DeactivationOutcomeDto(StudentDto student, boolean alreadyDeactivated, boolean emailSent) {
}
