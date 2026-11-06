package team.projectpulse.student;

import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Answers the questions a route asks about one named student: is that student the caller herself
 * ({@code isStudentSelf}), is the caller an instructor of that student's course section
 * ({@code isCurrentUserInstructorOfStudentSection}), and may the caller change that student's account lifecycle
 * ({@code canChangeStudentLifecycle}).
 *
 * <p>Every check fails closed: an id that names no row, or a link not yet established, is denied rather than
 * throwing. These methods run inside an {@code AuthorizationManager}, upstream of the {@code DispatcherServlet},
 * so an exception thrown here escapes the filter chain as a 500 that also tells the caller which ids exist.
 */
@Service
@Transactional
public class StudentSecurityService {

    private final StudentRepository studentRepository;
    private final UserUtils userUtils;


    public StudentSecurityService(StudentRepository studentRepository, UserUtils userUtils) {
        this.studentRepository = studentRepository;
        this.userUtils = userUtils;
    }

    public boolean isStudentSelf(Integer studentId) {
        Integer studentIdFromJwt = this.userUtils.getUserId();
        return studentId.equals(studentIdFromJwt);
    }

    /**
     * Check if the user is the instructor of the section that the student is in
     *
     * @param studentId the id of the student we need to access, this is NOT the current user's Id
     * @return
     */
    public boolean isCurrentUserInstructorOfStudentSection(Integer studentId) {
        Integer instructorIdFromJwt = this.userUtils.getUserId();
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }
        return student.getSection().getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(instructorIdFromJwt));
    }

    /**
     * May the caller deactivate (or reactivate) this student: is she an instructor assigned to the student's course
     * section, or the course admin of the course that section belongs to (BR-student-lifecycle,
     * BR-section-scoped-access)? Unlike {@link #isCurrentUserInstructorOfStudentSection}, this admits the course
     * admin even when she is not assigned to the course section (docs/design/stu.md, D4; the other routes' gap is
     * OI-65).
     */
    public boolean canChangeStudentLifecycle(Integer studentId) {
        Integer callerId = this.userUtils.getUserId();
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (callerId == null || student == null) {
            return false;
        }
        Section section = student.getSection();
        boolean assignedInstructor = section.getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(callerId));
        if (assignedInstructor) {
            return true;
        }
        Instructor courseAdmin = section.getCourse().getCourseAdmin();
        return courseAdmin != null && callerId.equals(courseAdmin.getId());
    }

}
