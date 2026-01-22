package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DockerHubSearchResponse {
    private List<DockerHubSummary> results;

    @Data
    public static class DockerHubSummary {
        private String name;
        @JsonProperty("short_description")
        private String shortDescription;
        @JsonProperty("star_count")
        private Integer starCount;
        private String source;
        @JsonProperty("logo_url")
        private LogoUrl logoUrl;
        private List<String> tags;

        @Data
        public static class LogoUrl {
            private String small;
            private String large;
        }
    }
}

