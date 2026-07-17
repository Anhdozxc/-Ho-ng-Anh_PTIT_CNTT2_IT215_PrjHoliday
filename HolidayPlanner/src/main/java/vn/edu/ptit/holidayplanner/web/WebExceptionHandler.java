package vn.edu.ptit.holidayplanner.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(basePackages = "vn.edu.ptit.holidayplanner.web")
public class WebExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ModelAndView notFound(EntityNotFoundException exception) {
        ModelAndView view = new ModelAndView("error/404");
        view.setStatus(HttpStatus.NOT_FOUND);
        view.addObject("message", exception.getMessage());
        return view;
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView forbidden(AccessDeniedException exception) {
        ModelAndView view = new ModelAndView("error/403");
        view.setStatus(HttpStatus.FORBIDDEN);
        view.addObject("message", exception.getMessage());
        return view;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Ảnh không được vượt quá 5 MB");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/admin/destinations")
                ? "redirect:/admin/destinations"
                : "redirect:/profile";
    }
}
