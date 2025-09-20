package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record AcademicStatusDto(
    @JsonProperty("gpa")
    BigDecimal gpa,
    
    @JsonProperty("gpaMax")
    BigDecimal gpaMax,
    
    @JsonProperty("completedCredits")
    Integer completedCredits,
    
    @JsonProperty("totalCreditsRequired")
    Integer totalCreditsRequired
) {}
