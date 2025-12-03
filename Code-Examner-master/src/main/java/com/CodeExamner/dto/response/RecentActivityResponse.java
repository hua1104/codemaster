// dto/response/RecentActivityResponse.java
package com.CodeExamner.dto.response;

import lombok.Data;

@Data
public class RecentActivityResponse {
    private String time;
    private String user;
    private String action;
}

