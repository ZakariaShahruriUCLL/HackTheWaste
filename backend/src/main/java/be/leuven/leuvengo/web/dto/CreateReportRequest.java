package be.leuven.leuvengo.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateReportRequest {

    @NotNull
    private Double lat;
    @NotNull
    private Double lng;
    @NotNull
    @Min(0) @Max(5)
    private Integer rating;
    private String note;
    private String imageRef;
    private String signalTags;
    private String facultyShortCode;
    private String reporterPseudoId;

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getImageRef() { return imageRef; }
    public void setImageRef(String imageRef) { this.imageRef = imageRef; }
    public String getSignalTags() { return signalTags; }
    public void setSignalTags(String signalTags) { this.signalTags = signalTags; }
    public String getFacultyShortCode() { return facultyShortCode; }
    public void setFacultyShortCode(String f) { this.facultyShortCode = f; }
    public String getReporterPseudoId() { return reporterPseudoId; }
    public void setReporterPseudoId(String reporterPseudoId) { this.reporterPseudoId = reporterPseudoId; }
}
