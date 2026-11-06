package team.projectpulse.security;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SEC-active-account: a session that was signed in when its account was deactivated is refused on its next
 * request, not when its token expires, and the account cannot sign in again. The check is account-generic, so it
 * covers an instructor as well as a student.
 *
 * <p>These tests flip {@code enabled} directly, so they exercise the cut-off on its own, independent of the
 * deactivation endpoint.
 */
@DisplayName("Integration tests for FR-SEC-active-account")
class ActiveAccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;


    private JSONObject login(String email) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(email, "123456"))).andReturn();
        return new JSONObject(mvcResult.getResponse().getContentAsString());
    }

    private String tokenOf(JSONObject loginResult) throws Exception {
        return "Bearer " + loginResult.getJSONObject("data").getString("token");
    }

    private int userIdOf(JSONObject loginResult) throws Exception {
        return loginResult.getJSONObject("data").getJSONObject("userInfo").getInt("id");
    }

    private void deactivate(int userId) {
        PeerEvaluationUser user = this.userRepository.findById(userId).orElseThrow();
        user.setEnabled(false);
        this.userRepository.saveAndFlush(user);
    }

    @Test
    void aDeactivatedStudentsLiveTokenIsRefusedOnItsNextRequest() throws Exception {
        JSONObject john = login("j.smith@abc.edu");
        String token = tokenOf(john);
        int johnId = userIdOf(john);

        this.mockMvc.perform(get(this.baseUrl + "/students/" + johnId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        deactivate(johnId);

        this.mockMvc.perform(get(this.baseUrl + "/students/" + johnId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));

        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void aDeactivatedInstructorsLiveTokenIsRefusedOnItsNextRequest() throws Exception {
        JSONObject bill = login("b.gates@abc.edu");
        String token = tokenOf(bill);
        int johnId = userIdOf(login("j.smith@abc.edu")); // A student in Bill's course section

        this.mockMvc.perform(get(this.baseUrl + "/students/" + johnId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        deactivate(userIdOf(bill));

        this.mockMvc.perform(get(this.baseUrl + "/students/" + johnId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

}
