package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TripPlanRequest {
    @NotBlank(message = "Tên chuyến đi không được để trống")
    @Size(max = 180, message = "Tên chuyến đi tối đa 180 ký tự")
    private String title;

    @NotNull(message="Vui lòng chọn điểm đến")
    private Long destinationId;

    @NotNull(message="Ngày bắt đầu là bắt buộc")
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message="Ngày kết thúc là bắt buộc")
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull @Min(value=1,message="Số người phải lớn hơn hoặc bằng 1")
    @Max(value=100,message="Số người tối đa là 100")
    private Integer peopleCount=1;

    @DecimalMin(value = "0.00", message = "Ngân sách không được âm")
    @Digits(integer = 13, fraction = 2, message = "Ngân sách tối đa 13 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal budget=BigDecimal.ZERO;

    @NotNull(message="Trạng thái là bắt buộc")
    private TripStatus status=TripStatus.DRAFT;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    private String notes;

    @AssertTrue(message = "Ngày kết thúc không được trước ngày bắt đầu")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    public String getTitle(){return title;} public void setTitle(String v){title=v == null ? null : v.trim();}
    public Long getDestinationId(){return destinationId;} public void setDestinationId(Long v){destinationId=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
    public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    public Integer getPeopleCount(){return peopleCount;} public void setPeopleCount(Integer v){peopleCount=v;}
    public BigDecimal getBudget(){return budget;} public void setBudget(BigDecimal v){budget=v;}
    public TripStatus getStatus(){return status;} public void setStatus(TripStatus v){status=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
