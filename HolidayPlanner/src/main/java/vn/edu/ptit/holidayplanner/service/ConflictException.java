package vn.edu.ptit.holidayplanner.service;

public class ConflictException extends IllegalArgumentException {
    public ConflictException(String message) {
        super(message);
    }
}
