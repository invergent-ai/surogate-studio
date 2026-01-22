package net.statemesh.k8s.task;

public record TaskConfig(Integer resourceOperationPollInterval,
                         Integer resourceOperationWaitTimeout,
                         Integer resourceOperationWatchTimeout,
                         Double requestVsLimitsCoefficientCpu,
                         Double requestVsLimitsCoefficientMemory,
                         Boolean deleteFinalizers,
                         Integer tokenTTL) {}
