package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class ChecklistRequest {
    @NotBlank(message = "Tên việc không được để trống")
    @Size(max = 250, message = "Tên việc tối đa 250 ký tự") private String title;
    @Size(max = 100, message = "Danh mục tối đa 100 ký tự") private String category;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate dueDate;
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;}
}
