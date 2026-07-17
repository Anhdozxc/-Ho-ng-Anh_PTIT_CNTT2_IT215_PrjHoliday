package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.NotNull;

public class DestinationStatusRequest {
    @NotNull(message = "Trạng thái hiển thị không được để trống")
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
