package vn.edu.ptit.holidayplanner.domain;

import jakarta.persistence.*;
import vn.edu.ptit.holidayplanner.domain.enums.BookingType;
import java.math.BigDecimal;

@Entity
@Table(name="booking_notes",indexes=@Index(name="idx_booking_trip",columnList="trip_plan_id"))
public class BookingNote {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="trip_plan_id",nullable=false) private TripPlan tripPlan;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private BookingType type;
    @Column(nullable=false,length=180) private String provider;
    @Column(length=120) private String bookingCode;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal price=BigDecimal.ZERO;
    @Column(length=1200) private String note;
    public Long getId(){return id;}
    public TripPlan getTripPlan(){return tripPlan;} public void setTripPlan(TripPlan v){tripPlan=v;}
    public BookingType getType(){return type;} public void setType(BookingType v){type=v;}
    public String getProvider(){return provider;} public void setProvider(String v){provider=v;}
    public String getBookingCode(){return bookingCode;} public void setBookingCode(String v){bookingCode=v;}
    public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;}
    public String getNote(){return note;} public void setNote(String v){note=v;}
}
