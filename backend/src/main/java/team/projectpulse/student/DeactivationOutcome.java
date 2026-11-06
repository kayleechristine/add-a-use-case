package team.projectpulse.student;

/**
 * The outcome of UC-STU-deactivate-student as the caller is told it: step 6 (deactivated, email sent), extension
 * 6a ({@code alreadyDeactivated}, no email), or extension 6b (deactivated, {@code emailSent} false).
 */
public record DeactivationOutcome(Student student, boolean alreadyDeactivated, boolean emailSent) {
}
