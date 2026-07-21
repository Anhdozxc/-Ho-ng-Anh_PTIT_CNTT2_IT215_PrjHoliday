package vn.edu.ptit.holidayplanner.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.holidayplanner.domain.enums.UserStatus;
import vn.edu.ptit.holidayplanner.service.TripPlanService;
import vn.edu.ptit.holidayplanner.service.UserService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WebErrorAndAuthenticationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TripPlanService tripPlanService;

    @Test
    void publicAuthenticationTemplatesRender() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("<form")));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(content().string(containsString("<form")));
    }

    @Test
    void faviconIsPublicAndServedWithIconContentType() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("image/x-icon")));
    }

    @Test
    void unauthenticatedWebRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void publicRegistrationSucceedsWithCsrfToken() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Public Registration")
                        .param("email", "  PUBLIC.REGISTRATION@EXAMPLE.COM ")
                        .param("password", "Public123")
                        .param("confirmPassword", "Public123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        var registered = userService.requireByEmail("public.registration@example.com");
        assertThat(registered.getFullName()).isEqualTo("Public Registration");
        assertThat(registered.getEmail()).isEqualTo("public.registration@example.com");
    }

    @Test
    void publicRegistrationWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .param("fullName", "Missing Csrf")
                        .param("email", "missing.csrf@example.com")
                        .param("password", "Missing123")
                        .param("confirmPassword", "Missing123"))
                .andExpect(status().isForbidden());

        assertThatThrownBy(() -> userService.requireByEmail("missing.csrf@example.com"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void tripCreateDetailAndEditTemplatesRenderForOwner() throws Exception {
        var owner = user("user@holidayplanner.vn").roles("USER");
        var trip = tripPlanService.listOwned("user@holidayplanner.vn").stream()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/trips/new").with(owner))
                .andExpect(status().isOk())
                .andExpect(view().name("trips/form"))
                .andExpect(model().attributeExists("tripRequest", "destinations"));

        mockMvc.perform(get("/trips/{id}", trip.getId()).with(owner))
                .andExpect(status().isOk())
                .andExpect(view().name("trips/detail"))
                .andExpect(model().attribute("editable", true))
                .andExpect(content().string(containsString(trip.getTitle())));

        mockMvc.perform(get("/trips/{id}/edit", trip.getId()).with(owner))
                .andExpect(status().isOk())
                .andExpect(view().name("trips/form"))
                .andExpect(model().attributeExists("tripRequest", "destinations"));
    }

    @Test
    void activeUserAuthenticatesThroughFormLoginAndReachesDashboard() throws Exception {
        mockMvc.perform(formLogin()
                        .user("user@holidayplanner.vn")
                        .password("user1234"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername("user@holidayplanner.vn"));
    }

    @Test
    void lockedUserCannotAuthenticateThroughFormLogin() throws Exception {
        var account = userService.requireByEmail("user@holidayplanner.vn");
        userService.setStatus(account.getId(), UserStatus.LOCKED, "admin@holidayplanner.vn");

        mockMvc.perform(formLogin()
                        .user("user@holidayplanner.vn")
                        .password("user1234"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void userOpeningAdminPageReceivesHttp403AndForwardsToBrandedPage() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .with(user("user@holidayplanner.vn").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/forbidden"));
    }

    @Test
    void unknownTripRenders404WithoutTechnicalDetails() throws Exception {
        mockMvc.perform(get("/trips/{id}", Long.MAX_VALUE)
                        .with(user("user@holidayplanner.vn").roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attributeExists("message"))
                .andExpect(content().string(containsString("404")))
                .andExpect(content().string(not(containsString("jakarta.persistence"))))
                .andExpect(content().string(not(containsString("stacktrace"))));
    }

    @Test
    void servletError403ResolvesBrandedTemplateWithCorrectStatus() throws Exception {
        mockMvc.perform(errorRequest(403, "/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(content().string(containsString("403")));
    }

    @Test
    void servletError500ResolvesBrandedTemplateWithoutLeakingException() throws Exception {
        mockMvc.perform(errorRequest(500, "/forced-failure")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION,
                                new IllegalStateException("sensitive-internal-marker")))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(content().string(containsString("500")))
                .andExpect(content().string(not(containsString("sensitive-internal-marker"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder errorRequest(
            int statusCode, String requestUri) {
        return get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, statusCode)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, requestUri)
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, "internal error");
    }
}
