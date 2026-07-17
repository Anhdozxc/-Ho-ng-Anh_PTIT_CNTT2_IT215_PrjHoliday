package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.NotNull;

public class ChecklistStateRequest {
    @NotNull(message = "Trạng thái checklist là bắt buộc")
    private Boolean done;

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }
}
