package vn.edu.ptit.holidayplanner.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Browsers often request {@code /favicon.ico} even when templates declare SVG icons.
 * Serve the bundled SVG asset to avoid 404 noise in network logs.
 */
@Controller
public class FaviconController {

    @GetMapping("/favicon.ico")
    public ResponseEntity<ClassPathResource> favicon() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .body(new ClassPathResource("static/images/favicon.svg"));
    }
}
