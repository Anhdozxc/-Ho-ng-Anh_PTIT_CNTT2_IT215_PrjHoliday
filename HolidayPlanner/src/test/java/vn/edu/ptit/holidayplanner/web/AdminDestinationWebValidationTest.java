package vn.edu.ptit.holidayplanner.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import vn.edu.ptit.holidayplanner.service.DestinationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDestinationWebValidationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private DestinationService destinationService;

    @Test
    void invalidCreateRendersInlineErrorsAndKeepsSubmittedDataAndListContext() throws Exception {
        mockMvc.perform(post("/admin/destinations")
                        .characterEncoding("UTF-8")
                        .with(user("admin@holidayplanner.vn").roles("ADMIN"))
                        .with(csrf())
                        .param("name", " ")
                        .param("city", "Thành phố đã nhập")
                        .param("country", "Việt Nam")
                        .param("imageUrl", "not-a-url")
                        .param("description", "Mô tả đang nhập")
                        .param("active", "true")
                        .param("q", "Đà")
                        .param("filterActive", "true")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/destinations"))
                .andExpect(model().attributeHasFieldErrors("destinationRequest", "name", "imageUrl"))
                .andExpect(model().attribute("destinationRequest",
                        hasProperty("city", equalTo("Thành phố đã nhập"))))
                .andExpect(model().attribute("destinationRequest",
                        hasProperty("description", equalTo("Mô tả đang nhập"))))
                .andExpect(model().attribute("q", "Đà"))
                .andExpect(model().attribute("selectedActive", true))
                .andExpect(content().string(containsString("destinationNameError")))
                .andExpect(content().string(containsString("value=\"not-a-url\"")));
    }

    @Test
    void invalidUpdateRendersDedicatedEditPanelWithEverySubmittedField() throws Exception {
        long id = destinationService.activeDestinations().get(0).getId();

        mockMvc.perform(post("/admin/destinations/{id}", id)
                        .characterEncoding("UTF-8")
                        .with(user("admin@holidayplanner.vn").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "Tên điểm đến vừa nhập")
                        .param("city", "Thành phố vừa nhập")
                        .param("country", " ")
                        .param("imageUrl", "https://example.test/retained.webp")
                        .param("description", "Mô tả cập nhật chưa hợp lệ")
                        .param("active", "false")
                        .param("q", "Tokyo")
                        .param("filterActive", "false")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/destinations"))
                .andExpect(model().attributeHasFieldErrors("editDestinationRequest", "country"))
                .andExpect(model().attribute("editDestinationRequest",
                        hasProperty("name", equalTo("Tên điểm đến vừa nhập"))))
                .andExpect(model().attribute("editDestinationRequest",
                        hasProperty("description", equalTo("Mô tả cập nhật chưa hợp lệ"))))
                .andExpect(model().attribute("editDestinationId", id))
                .andExpect(model().attribute("q", "Tokyo"))
                .andExpect(model().attribute("selectedActive", false))
                .andExpect(content().string(containsString("data-edit-validation-panel")))
                .andExpect(content().string(containsString("invalidEditDestinationCountryError")))
                .andExpect(content().string(containsString("https://example.test/retained.webp")));
    }

    @Test
    void rejectedUpdateImageIsShownInlineWithoutLosingMetadata() throws Exception {
        long id = destinationService.activeDestinations().get(0).getId();
        MockMultipartFile invalidImage = new MockMultipartFile(
                "imageFile", "notes.txt", "text/plain", "not-an-image".getBytes());

        MvcResult mvcResult = mockMvc.perform(multipart("/admin/destinations/{id}", id)
                        .file(invalidImage)
                        .characterEncoding("UTF-8")
                        .with(user("admin@holidayplanner.vn").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "Tên vẫn được giữ")
                        .param("city", "Hà Nội")
                        .param("country", "Việt Nam")
                        .param("imageUrl", "https://example.test/fallback.webp")
                        .param("description", "Không mất nội dung này")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/destinations"))
                .andExpect(model().attributeHasErrors("editDestinationRequest"))
                .andExpect(model().attribute("editDestinationRequest",
                        hasProperty("name", equalTo("Tên vẫn được giữ"))))
                .andExpect(model().attribute("editDestinationRequest",
                        hasProperty("description", equalTo("Không mất nội dung này"))))
                .andExpect(content().string(containsString("data-edit-validation-panel")))
                .andReturn();

        BindingResult bindingResult = (BindingResult) mvcResult.getModelAndView().getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + "editDestinationRequest");
        assertThat(bindingResult.getGlobalError()).isNotNull();
        assertThat(bindingResult.getGlobalError().getDefaultMessage())
                .isEqualTo("Ảnh chỉ hỗ trợ định dạng JPG, JPEG, PNG hoặc WEBP");
    }
}
