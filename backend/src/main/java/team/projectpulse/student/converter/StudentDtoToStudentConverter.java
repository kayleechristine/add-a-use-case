package team.projectpulse.student.converter;

import team.projectpulse.student.Student;
import team.projectpulse.student.dto.StudentDto;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StudentDtoToStudentConverter implements Converter<StudentDto, Student> {

    @Override
    public Student convert(StudentDto studentDto) {
        Student student = new Student();
        student.setId(studentDto.id());
        student.setUsername(studentDto.username());
        // enabled is not mapped: StudentService.updateStudent takes it separately, because null means "no change" (D5)
        student.setFirstName(studentDto.firstName());
        student.setLastName(studentDto.lastName());
        student.setEmail(studentDto.email());
        return student;
    }

}
