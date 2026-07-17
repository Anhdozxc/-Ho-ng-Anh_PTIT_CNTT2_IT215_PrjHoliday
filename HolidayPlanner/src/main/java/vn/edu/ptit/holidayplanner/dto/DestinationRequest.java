package vn.edu.ptit.holidayplanner.dto;

import jakarta.validation.constraints.*;

public class DestinationRequest {
    @NotBlank(message="Tên điểm đến không được để trống")
    @Size(max=140, message="Tên điểm đến tối đa 140 ký tự") private String name;
    @Size(max=120, message="Thành phố tối đa 120 ký tự") private String city;
    @NotBlank(message="Quốc gia không được để trống")
    @Size(max=120, message="Quốc gia tối đa 120 ký tự") private String country;
    @Size(max=1500, message="Mô tả tối đa 1500 ký tự") private String description;
    @Size(max=700, message="URL ảnh tối đa 700 ký tự")
    @Pattern(regexp="^(https?://[^\\s\"'<>\\\\]+|/[^\\s\"'<>\\\\]*)?$", message="Ảnh phải là URL HTTP(S) hoặc đường dẫn local bắt đầu bằng /")
    private String imageUrl;
    private boolean active=true;
    public String getName(){return name;} public void setName(String v){name=trim(v);}
    public String getCity(){return city;} public void setCity(String v){city=trim(v);}
    public String getCountry(){return country;} public void setCountry(String v){country=trim(v);}
    public String getDescription(){return description;} public void setDescription(String v){description=trim(v);}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=trim(v);}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    private String trim(String value){return value == null ? null : value.trim();}
}
