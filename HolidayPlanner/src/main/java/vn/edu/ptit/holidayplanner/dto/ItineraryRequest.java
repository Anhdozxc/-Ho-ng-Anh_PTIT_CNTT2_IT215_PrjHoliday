package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalTime;

public class ItineraryRequest {
    @NotNull(message = "Ngày thứ là bắt buộc")
    @Min(value = 1, message = "Ngày thứ phải từ 1")
    private Integer dayNo;
    @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) private LocalTime fromTime;
    @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) private LocalTime toTime;
    @NotBlank(message = "Hoạt động không được để trống")
    @Size(max = 250, message = "Hoạt động tối đa 250 ký tự")
    private String activity;
    @Size(max = 250, message = "Địa điểm tối đa 250 ký tự") private String location;
    @Size(max = 1200, message = "Ghi chú tối đa 1200 ký tự") private String note;

    @AssertTrue(message = "Giờ bắt đầu và giờ kết thúc phải được nhập cùng nhau")
    public boolean isTimeRangeComplete() {
        return (fromTime == null) == (toTime == null);
    }

    @AssertTrue(message = "Thời gian kết thúc phải sau thời gian bắt đầu")
    public boolean isTimeRangeOrdered() {
        return fromTime == null || toTime == null || toTime.isAfter(fromTime);
    }
    public Integer getDayNo(){return dayNo;} public void setDayNo(Integer v){dayNo=v;}
    public LocalTime getFromTime(){return fromTime;} public void setFromTime(LocalTime v){fromTime=v;}
    public LocalTime getToTime(){return toTime;} public void setToTime(LocalTime v){toTime=v;}
    public String getActivity(){return activity;} public void setActivity(String v){activity=v;}
    public String getLocation(){return location;} public void setLocation(String v){location=v;}
    public String getNote(){return note;} public void setNote(String v){note=v;}
}
