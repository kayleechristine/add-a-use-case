package team.projectpulse.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer>, JpaSpecificationExecutor<Student> {

    // Find all students in a section
    List<Student> findBySectionSectionId(Integer sectionId);

    List<Student> findByTeamTeamId(Integer teamId);

    /**
     * Deactivates the student only if she is still active, in one conditional statement, so that of two concurrent
     * deactivations exactly one changes the row (UC-STU-deactivate-student, extension 6a). Team membership is not
     * touched (BR-student-lifecycle).
     *
     * @return the number of rows changed: 1 if she was active, 0 if she was already deactivated or does not exist
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Student s SET s.enabled = false WHERE s.id = :studentId AND s.enabled = true")
    int deactivateIfActive(@Param("studentId") Integer studentId);
}
