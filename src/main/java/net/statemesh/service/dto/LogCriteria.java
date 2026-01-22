package net.statemesh.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class LogCriteria {
    private String applicationId;
    private Integer limit;
    private String searchTerm;
    private Integer sinceSeconds;
    private Instant startDate;
    private Instant endDate;

    public LogCriteria() {}
}
