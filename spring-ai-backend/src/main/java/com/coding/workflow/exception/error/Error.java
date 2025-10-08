package com.coding.workflow.exception.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Error implements Serializable {
    /** HTTP status code of the error */
    @JsonProperty("status_code")
    private Integer statusCode;

    /** Type of the error */
    private String type;

    /** Error code */
    private String code;

    /** Error message */
    private String message;

    /** Parameter that caused the error */
    private String param;
}