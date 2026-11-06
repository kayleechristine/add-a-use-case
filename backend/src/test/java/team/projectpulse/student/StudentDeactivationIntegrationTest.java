package team.projectpulse.student;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.section.Section;
import team.projectpulse.section.SectionRepository;
import team.projectpulse.system.EmailService;
import team.projectpulse.system.StatusCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC-STU-deactivate-student against the seed data. John Smith (id 4) is on Team1 in course section 2. Bill Gates
 * is assigned to course section 2 but owns no course; Bingyang Wei owns its course and is assigned to it; Tim Cook
 * owns another course and is not assigned to it.
 */
@DisplayName("Integration tests for UC-STU-deactivate-student")
class StudentDeactivationIntegrationTest extends AbstractIntegrationTest {

    private static final int JOHN = 4;

    @MockitoSpyBean
    EmailService emailService; // Real sends to the Mailpit container; the spy records them

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    String instructorBillToken;
    String adminBingyangToken;
    String adminTimToken;
    String studentEricToken;


    @BeforeEach
    void setUp() throws Exception {
        this.instructorBillToken = login("b.gates@abc.edu");
        this.adminBingyangToken = login("b.wei@abc.edu");
        this.adminTimToken = login("t.cook@abc.edu");
        this.studentEricToken = login("e.hudson@abc.edu");
    }

    private String login(String email) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(email, "123456"))).andReturn();
        return "Bearer " + new JSONObject(mvcResult.getResponse().getContentAsString()).getJSONObject("data").getString("token");
    }

    private ResultActions deactivate(int studentId, String token) throws Exception {
        return this.mockMvc.perform(patch(this.baseUrl + "/students/" + studentId + "/deactivation").header(HttpHeaders.AUTHORIZATION, token));
    }

    private Student john() {
        return this.studentRepository.findById(JOHN).orElseThrow();
    }

    private void verifyEmailsToJohn(int count) {
        verify(this.emailService, times(count)).sendDeactivationEmail(argThat(student -> "j.smith@abc.edu".equals(student.getEmail())));
    }

    @Test
    void theAssignedInstructorDeactivatesJohn() throws Exception {
        // Main success scenario, step 6
        deactivate(JOHN, this.instructorBillToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("John Smith has been deactivated."))
                .andExpect(jsonPath("$.data.student.enabled").value(false)) // D3: the re-read, not the stale copy
                .andExpect(jsonPath("$.data.student.teamName").value("Team1"))
                .andExpect(jsonPath("$.data.alreadyDeactivated").value(false))
                .andExpect(jsonPath("$.data.emailSent").value(true));

        assertThat(john().isEnabled()).isFalse();
        assertThat(john().getTeam().getTeamName()).isEqualTo("Team1"); // BR-student-lifecycle: team untouched
        verifyEmailsToJohn(1);
    }

    @Test
    void theCourseAdminDeactivatesJohnWithoutBeingAssignedToHisCourseSection() throws Exception {
        // D4: Bingyang owns the course; unassign her from course section 2 first
        Section section = this.sectionRepository.findById(2).orElseThrow();
        section.getInstructors().removeIf(instructor -> instructor.getId() == 1);
        this.sectionRepository.saveAndFlush(section);

        deactivate(JOHN, this.adminBingyangToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("John Smith has been deactivated."));

        assertThat(john().isEnabled()).isFalse();
    }

    @Test
    void anUnauthorizedInstructorCannotDeactivateJohn() throws Exception {
        // 6c: Tim is neither assigned to course section 2 nor the course admin of its course
        deactivate(JOHN, this.adminTimToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."));

        assertThat(john().isEnabled()).isTrue();
        verify(this.emailService, never()).sendDeactivationEmail(any());
    }

    @Test
    void anIdNamingNoStudentIsForbidden() throws Exception {
        // 6c, fail-closed manager
        deactivate(999999, this.instructorBillToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No permission."));
    }

    @Test
    void aStudentCannotDeactivateAStudent() throws Exception {
        // FR-SEC-authorization
        deactivate(JOHN, this.studentEricToken)
                .andExpect(status().isForbidden());

        assertThat(john().isEnabled()).isTrue();
    }

    @Test
    void deactivatingAnAlreadyDeactivatedStudentChangesNothing() throws Exception {
        // 6a
        deactivate(JOHN, this.instructorBillToken).andExpect(status().isOk());

        deactivate(JOHN, this.adminBingyangToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.message").value("John Smith was already deactivated."))
                .andExpect(jsonPath("$.data.alreadyDeactivated").value(true))
                .andExpect(jsonPath("$.data.emailSent").value(false))
                .andExpect(jsonPath("$.data.student.enabled").value(false));

        verifyEmailsToJohn(1); // Only the first deactivation sent one
    }

    @Test
    void aFailedEmailKeepsJohnDeactivated() throws Exception {
        // 6b
        willThrow(new RuntimeException("SMTP unavailable")).given(this.emailService).sendDeactivationEmail(any());

        deactivate(JOHN, this.instructorBillToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.message").value("John Smith has been deactivated, but the notification email could not be sent."))
                .andExpect(jsonPath("$.data.emailSent").value(false))
                .andExpect(jsonPath("$.data.student.enabled").value(false));

        assertThat(john().isEnabled()).isFalse();
    }

    @Test
    void johnsLiveSessionIsCutOffByTheDeactivation() throws Exception {
        // FR-SEC-active-account, end to end through the endpoint
        String johnToken = login("j.smith@abc.edu");

        deactivate(JOHN, this.instructorBillToken).andExpect(status().isOk());

        this.mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(this.baseUrl + "/students/" + JOHN).header(HttpHeaders.AUTHORIZATION, johnToken))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized());
    }

}
