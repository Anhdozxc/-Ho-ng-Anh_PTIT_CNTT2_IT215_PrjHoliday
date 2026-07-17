package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {
    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void registrationRequiresMeaningfulNameStrongPasswordAndMatchingConfirmation() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("---");
        request.setEmail("USER@EXAMPLE.COM");
        request.setPassword("abcdefgh");
        request.setConfirmPassword("different1");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(messages(violations))
                .contains("Họ tên phải chứa chữ hoặc số")
                .contains("Mật khẩu phải có cả chữ và số")
                .contains("Xác nhận mật khẩu không khớp");
        assertThat(violations).anySatisfy(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("confirmPassword");
            assertThat(violation.getMessage()).isEqualTo("Xác nhận mật khẩu không khớp");
        });

        request.setFullName("  An Nguyễn  ");
        request.setEmail("  USER@EXAMPLE.COM  ");
        request.setPassword("secure123");
        request.setConfirmPassword("secure123");
        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.getFullName()).isEqualTo("An Nguyễn");
        assertThat(request.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void changePasswordRequiresLettersDigitsAndMatchingConfirmation() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("12345678");
        request.setConfirmPassword("87654321");

        assertThat(messages(validator.validate(request)))
                .contains("Mật khẩu mới phải có cả chữ và số")
                .contains("Xác nhận mật khẩu mới không khớp");
    }

    @Test
    void itineraryTimesMustBeCompleteAndOrdered() {
        ItineraryRequest request = new ItineraryRequest();
        request.setDayNo(1);
        request.setActivity("Tham quan");
        request.setFromTime(LocalTime.of(9, 0));

        assertThat(messages(validator.validate(request)))
                .contains("Giờ bắt đầu và giờ kết thúc phải được nhập cùng nhau");

        request.setToTime(LocalTime.of(8, 0));
        assertThat(messages(validator.validate(request)))
                .contains("Thời gian kết thúc phải sau thời gian bắt đầu");
    }

    @Test
    void moneyFieldsRejectDatabasePrecisionOverflow() {
        ExpenseRequest expense = new ExpenseRequest();
        expense.setAmount(new BigDecimal("100.001"));
        BookingNoteRequest booking = new BookingNoteRequest();
        booking.setPrice(new BigDecimal("10000000000000.00"));

        assertThat(messages(validator.validate(expense)))
                .contains("Số tiền tối đa 13 chữ số nguyên và 2 chữ số thập phân");
        assertThat(messages(validator.validate(booking)))
                .contains("Chi phí tối đa 13 chữ số nguyên và 2 chữ số thập phân");
    }

    private <T> Set<String> messages(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
    }
}
