package vn.edu.ptit.holidayplanner.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="checklist_items",indexes=@Index(name="idx_checklist_trip",columnList="trip_plan_id"))
public class ChecklistItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="trip_plan_id",nullable=false) private TripPlan tripPlan;
    @Column(nullable=false,length=250) private String title;
    @Column(length=100) private String category;
    @Column(nullable=false) private boolean done=false;
    private LocalDate dueDate;
    public Long getId(){return id;}
    public TripPlan getTripPlan(){return tripPlan;} public void setTripPlan(TripPlan v){tripPlan=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public boolean isDone(){return done;} public void setDone(boolean v){done=v;}
    public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;}
}
