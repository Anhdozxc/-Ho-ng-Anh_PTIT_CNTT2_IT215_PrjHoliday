package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.*;
import vn.edu.ptit.holidayplanner.domain.enums.BookingType;
import java.math.BigDecimal;

public class BookingNoteRequest {
    @NotNull(message = "Loại dịch vụ là bắt buộc") private BookingType type;
    @NotBlank(message = "Nhà cung cấp không được để trống")
    @Size(max = 180, message = "Nhà cung cấp tối đa 180 ký tự") private String provider;
    @Size(max = 120, message = "Mã đặt chỗ tối đa 120 ký tự") private String bookingCode;
    @NotNull(message = "Chi phí là bắt buộc")
    @DecimalMin(value = "0.00", message = "Chi phí không được âm")
    @Digits(integer = 13, fraction = 2, message = "Chi phí tối đa 13 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal price = BigDecimal.ZERO;
    @Size(max = 1200, message = "Ghi chú tối đa 1200 ký tự") private String note;
    public BookingType getType(){return type;} public void setType(BookingType v){type=v;}
    public String getProvider(){return provider;} public void setProvider(String v){provider=v;}
    public String getBookingCode(){return bookingCode;} public void setBookingCode(String v){bookingCode=v;}
    public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;}
    public String getNote(){return note;} public void setNote(String v){note=v;}
}
