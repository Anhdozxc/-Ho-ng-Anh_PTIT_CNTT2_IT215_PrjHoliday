package vn.edu.ptit.holidayplanner.domain;

import jakarta.persistence.*;
import vn.edu.ptit.holidayplanner.domain.enums.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="expenses",indexes=@Index(name="idx_expense_trip",columnList="trip_plan_id"))
public class Expense {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="trip_plan_id",nullable=false) private TripPlan tripPlan;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private ExpenseCategory category;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal amount;
    private LocalDate spentDate;
    @Column(length=1200) private String note;
    public Long getId(){return id;}
    public TripPlan getTripPlan(){return tripPlan;} public void setTripPlan(TripPlan v){tripPlan=v;}
    public ExpenseCategory getCategory(){return category;} public void setCategory(ExpenseCategory v){category=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public LocalDate getSpentDate(){return spentDate;} public void setSpentDate(LocalDate v){spentDate=v;}
    public String getNote(){return note;} public void setNote(String v){note=v;}
}
