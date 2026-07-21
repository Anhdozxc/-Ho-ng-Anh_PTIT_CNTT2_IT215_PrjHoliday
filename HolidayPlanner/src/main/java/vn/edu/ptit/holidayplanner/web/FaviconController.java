package vn.edu.ptit.holidayplanner.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves a real multi-size ICO file for browsers that request {@code /favicon.ico}.
 * The SVG icon declared in the shared fragment remains available for modern browsers.
 */
@Controller
public class FaviconController {

    private static final MediaType ICON_MEDIA_TYPE = MediaType.parseMediaType("image/x-icon");

    @GetMapping("/favicon.ico")
    public ResponseEntity<ClassPathResource> favicon() {
        return ResponseEntity.ok()
                .contentType(ICON_MEDIA_TYPE)
                .body(new ClassPathResource("static/favicon.ico"));
    }
}
