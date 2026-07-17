package vn.edu.ptit.holidayplanner.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name="itinerary_items",indexes=@Index(name="idx_itinerary_trip",columnList="trip_plan_id"))
public class ItineraryItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="trip_plan_id",nullable=false) private TripPlan tripPlan;
    @Column(nullable=false) private Integer dayNo;
    private LocalTime fromTime;
    private LocalTime toTime;
    @Column(nullable=false,length=250) private String activity;
    @Column(length=250) private String location;
    @Column(length=1200) private String note;
    public Long getId(){return id;}
    public TripPlan getTripPlan(){return tripPlan;} public void setTripPlan(TripPlan v){tripPlan=v;}
    public Integer getDayNo(){return dayNo;} public void setDayNo(Integer v){dayNo=v;}
    public LocalTime getFromTime(){return fromTime;} public void setFromTime(LocalTime v){fromTime=v;}
    public LocalTime getToTime(){return toTime;} public void setToTime(LocalTime v){toTime=v;}
    public String getActivity(){return activity;} public void setActivity(String v){activity=v;}
    public String getLocation(){return location;} public void setLocation(String v){location=v;}
    public String getNote(){return note;} public void setNote(String v){note=v;}
}
