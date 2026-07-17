package vn.edu.ptit.holidayplanner.api;

import vn.edu.ptit.holidayplanner.domain.*;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiMapper {
    private ApiMapper() {}

    public static Map<String,Object> destination(Destination d) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", d.getId()); m.put("name", d.getName()); m.put("city", d.getCity());
        m.put("country", d.getCountry()); m.put("description", d.getDescription());
        m.put("imageUrl", d.getImageUrl()); m.put("active", d.isActive());
        return m;
    }

    public static Map<String,Object> trip(TripPlan t) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", t.getId()); m.put("title", t.getTitle());
        m.put("destination", destination(t.getDestination()));
        m.put("startDate", t.getStartDate()); m.put("endDate", t.getEndDate());
        m.put("peopleCount", t.getPeopleCount()); m.put("budget", t.getBudget());
        m.put("status", t.getStatus()); m.put("notes", t.getNotes());
        Map<String,Object> owner = new LinkedHashMap<>();
        owner.put("id", t.getOwner().getId()); owner.put("fullName", t.getOwner().getFullName());
        owner.put("email", t.getOwner().getEmail()); owner.put("avatarUrl", t.getOwner().getAvatarUrl());
        m.put("owner", owner);
        m.put("createdAt", t.getCreatedAt()); m.put("updatedAt", t.getUpdatedAt());
        return m;
    }

    public static Map<String,Object> itinerary(ItineraryItem i) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", i.getId()); m.put("dayNo", i.getDayNo()); m.put("fromTime", i.getFromTime());
        m.put("toTime", i.getToTime()); m.put("activity", i.getActivity());
        m.put("location", i.getLocation()); m.put("note", i.getNote());
        return m;
    }

    public static Map<String,Object> expense(Expense e) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("category", e.getCategory()); m.put("amount", e.getAmount());
        m.put("spentDate", e.getSpentDate()); m.put("note", e.getNote());
        return m;
    }

    public static Map<String,Object> checklist(ChecklistItem c) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("title", c.getTitle()); m.put("category", c.getCategory());
        m.put("done", c.isDone()); m.put("dueDate", c.getDueDate());
        return m;
    }

    public static Map<String,Object> booking(BookingNote b) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", b.getId()); m.put("type", b.getType()); m.put("provider", b.getProvider());
        m.put("bookingCode", b.getBookingCode()); m.put("price", b.getPrice()); m.put("note", b.getNote());
        return m;
    }

    public static Map<String,Object> user(UserAccount u) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", u.getId()); m.put("fullName", u.getFullName()); m.put("email", u.getEmail());
        m.put("avatarUrl", u.getAvatarUrl()); m.put("role", u.getRole());
        m.put("status", u.getStatus()); m.put("createdAt", u.getCreatedAt());
        return m;
    }
}
