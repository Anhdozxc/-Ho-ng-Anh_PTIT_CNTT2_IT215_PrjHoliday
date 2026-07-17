package vn.edu.ptit.holidayplanner.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="destinations")
public class Destination {
    public static final String DEFAULT_IMAGE_URL = "/images/destination-fallback.svg";

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=140) private String name;
    @Column(length=120) private String city;
    @Column(nullable=false,length=120) private String country;
    @Column(length=1500) private String description;
    @Column(length=700) private String imageUrl;
    @Column(name="image_public_id",length=255) private String imagePublicId;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false,updatable=false) private LocalDateTime createdAt=LocalDateTime.now();
    public Long getId(){return id;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getCity(){return city;} public void setCity(String v){city=v;}
    public String getCountry(){return country;} public void setCountry(String v){country=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getImageUrl(){return imageUrl == null || imageUrl.isBlank() ? DEFAULT_IMAGE_URL : imageUrl;}
    public void setImageUrl(String v){imageUrl=v;}
    public String getImagePublicId(){return imagePublicId;} public void setImagePublicId(String v){imagePublicId=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
