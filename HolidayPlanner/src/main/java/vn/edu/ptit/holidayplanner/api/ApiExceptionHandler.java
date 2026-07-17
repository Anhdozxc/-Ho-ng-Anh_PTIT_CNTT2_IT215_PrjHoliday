package vn.edu.ptit.holidayplanner.api;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import vn.edu.ptit.holidayplanner.service.ConflictException;
import vn.edu.ptit.holidayplanner.media.ImageStorageException;
import vn.edu.ptit.holidayplanner.media.ImageStorageUnavailableException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "vn.edu.ptit.holidayplanner.api")
public class ApiExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> notFound(EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> forbidden(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> conflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> dataConflict(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "Dữ liệu xung đột với bản ghi hiện có");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> malformedRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu hoặc tham số không đúng định dạng");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        Map<String,String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(
                ApiErrorResponse.body(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", fields));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> uploadTooLarge(MaxUploadSizeExceededException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh không được vượt quá 5 MB");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<?> missingUpload(MissingServletRequestPartException exception) {
        return error(HttpStatus.BAD_REQUEST, "Vui lòng chọn một file ảnh");
    }

    @ExceptionHandler(ImageStorageUnavailableException.class)
    public ResponseEntity<?> storageUnavailable(ImageStorageUnavailableException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<?> storageFailure(ImageStorageException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiErrorResponse.body(status, message));
    }
}
