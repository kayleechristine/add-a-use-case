package team.projectpulse.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

/**
 * UC-STU-deactivate-student 6c at the unit level: who may change a student's account lifecycle
 * (BR-student-lifecycle, BR-section-scoped-access).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentSecurityService.canChangeStudentLifecycle")
class StudentSecurityServiceTest {

    @Mock
    StudentRepository studentRepository;

    @Mock
    UserUtils userUtils;

    @InjectMocks
    StudentSecurityService studentSecurityService;


    @BeforeEach
    void setUp() {
        Instructor courseAdmin = new Instructor("b.wei@abc.edu", "Bingyang", "Wei", "b.wei@abc.edu", "123456", true, "admin instructor");
        courseAdmin.setId(1);
        Instructor assigned = new Instructor("b.gates@abc.edu", "Bill", "Gates", "b.gates@abc.edu", "123456", true, "instructor");
        assigned.setId(2);

        Course course = new Course("COSC 40993 Senior Design", "Senior design");
        course.setCourseAdmin(courseAdmin);
        Section section = new Section("2023-2024", LocalDate.of(2023, 8, 14), LocalDate.of(2024, 4, 29), true, DayOfWeek.MONDAY, LocalTime.of(23, 59), DayOfWeek.TUESDAY, LocalTime.of(23, 59));
        section.setCourse(course);
        section.addInstructor(assigned); // The course admin is deliberately not assigned

        Student john = new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", true, "student");
        john.setId(4);
        section.addStudent(john);
        lenient().when(this.studentRepository.findById(4)).thenReturn(Optional.of(john)); // Not every test names John
    }

    @Test
    void anAssignedInstructorMay() {
        given(this.userUtils.getUserId()).willReturn(2);
        assertThat(this.studentSecurityService.canChangeStudentLifecycle(4)).isTrue();
    }

    @Test
    void theCourseAdminMayWithoutBeingAssigned() {
        given(this.userUtils.getUserId()).willReturn(1);
        assertThat(this.studentSecurityService.canChangeStudentLifecycle(4)).isTrue();
    }

    @Test
    void anyoneElseMayNot() {
        given(this.userUtils.getUserId()).willReturn(3);
        assertThat(this.studentSecurityService.canChangeStudentLifecycle(4)).isFalse();
    }

    @Test
    void anIdNamingNoStudentIsDenied() {
        given(this.userUtils.getUserId()).willReturn(2);
        given(this.studentRepository.findById(999999)).willReturn(Optional.empty());
        assertThat(this.studentSecurityService.canChangeStudentLifecycle(999999)).isFalse();
    }

}
