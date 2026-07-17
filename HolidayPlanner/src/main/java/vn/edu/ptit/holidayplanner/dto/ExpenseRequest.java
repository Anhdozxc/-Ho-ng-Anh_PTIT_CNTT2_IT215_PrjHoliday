package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseRequest {
    @NotNull(message = "Danh mục là bắt buộc")
    private ExpenseCategory category;
    @NotNull(message = "Số tiền là bắt buộc")
    @DecimalMin(value = "0.00", message = "Số tiền không được âm")
    @Digits(integer = 13, fraction = 2,
            message = "Số tiền tối đa 13 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal amount;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate spentDate;
    @Size(max = 1200, message = "Ghi chú tối đa 1200 ký tự")
    private String note;

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory v) {
        category = v;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal v) {
        amount = v;
    }

    public LocalDate getSpentDate() {
        return spentDate;
    }

    public void setSpentDate(LocalDate v) {
        spentDate = v;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String v) {
        note = v;
    }
}
