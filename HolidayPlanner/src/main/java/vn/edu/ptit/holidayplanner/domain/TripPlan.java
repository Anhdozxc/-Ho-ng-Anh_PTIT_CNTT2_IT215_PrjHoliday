package vn.edu.ptit.holidayplanner.domain;

import jakarta.persistence.*;
import vn.edu.ptit.holidayplanner.domain.enums.TripStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="trip_plans", indexes={
    @Index(name="idx_trip_owner",columnList="owner_id"),
    @Index(name="idx_trip_start_date",columnList="start_date"),
    @Index(name="idx_trip_status",columnList="status")
})
public class TripPlan {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=180) private String title;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="destination_id",nullable=false) private Destination destination;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date",nullable=false) private LocalDate endDate;
    @Column(nullable=false) private Integer peopleCount;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal budget=BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TripStatus status=TripStatus.DRAFT;
    @Column(length=2000) private String notes;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="owner_id",nullable=false) private UserAccount owner;
    @Column(nullable=false) private boolean deleted=false;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt=LocalDateTime.now();
    @Column(nullable=false) private LocalDateTime updatedAt=LocalDateTime.now();
    @PreUpdate public void preUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public Destination getDestination(){return destination;} public void setDestination(Destination v){destination=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
    public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    public Integer getPeopleCount(){return peopleCount;} public void setPeopleCount(Integer v){peopleCount=v;}
    public BigDecimal getBudget(){return budget;} public void setBudget(BigDecimal v){budget=v;}
    public TripStatus getStatus(){return status;} public void setStatus(TripStatus v){status=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public UserAccount getOwner(){return owner;} public void setOwner(UserAccount v){owner=v;}
    public boolean isDeleted(){return deleted;} public void setDeleted(boolean v){deleted=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
