package team.projectpulse.student;

/**
 * What the state change of a deactivation did, before any email is sent: the student as she now is, and whether
 * she was already deactivated, in which case nothing changed (UC-STU-deactivate-student, extension 6a).
 */
public record DeactivationResult(Student student, boolean alreadyDeactivated) {
}
