package net.statemesh.web.rest.vm;

import lombok.Data;

@Data
public class BootstrapRequest {
    String nodeIp;
    String userKey;
    String machineId;
}
