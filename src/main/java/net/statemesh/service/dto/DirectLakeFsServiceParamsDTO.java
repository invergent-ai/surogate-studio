package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DirectLakeFsServiceParamsDTO {
    String endpoint;
    String auth;
    String s3Auth;
    String s3Endpoint;
}
