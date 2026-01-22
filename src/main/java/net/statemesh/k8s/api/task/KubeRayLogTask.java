package net.statemesh.k8s.api.task;

import com.amazonaws.util.StringInputStream;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.api.KubeRayClient;
import net.statemesh.k8s.api.model.RayJobDetails;
import net.statemesh.k8s.exception.APIException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class KubeRayLogTask {
    private final ApiStub apiStub;
    private final String rayCluster;
    private final String submissionId;
    private final Integer tailLines;
    private final Integer sinceSeconds; // No support for it yet

    public KubeRayLogTask(ApiStub apiStub,
                          String rayCluster,
                          String submissionId,
                          Integer tailLines,
                          Integer sinceSeconds) {
        this.apiStub = apiStub;
        this.rayCluster = rayCluster;
        this.submissionId = submissionId;
        this.tailLines = tailLines;
        this.sinceSeconds = sinceSeconds;
    }

    public CompletableFuture<TaskResult<InputStream>> call() {
        if (!this.apiStub.getKubeRayClients().containsKey(rayCluster)) {
            throw new APIException("KubeRay client for ray cluster " + rayCluster + " was not configured");
        }
        final KubeRayClient client = this.apiStub.getKubeRayClients().get(rayCluster);
        final RayJobDetails jobDetails = client.getJobDetails(submissionId);
        if (jobDetails == null) {
            throw new APIException("Could not get job details for submission " + submissionId);
        }

        final String logs = client.getLogs(jobDetails.getSubmissionId(), jobDetails.getDriverAgentNodeId(), tailLines);
        try {
            return CompletableFuture.completedFuture(
                TaskResult.<InputStream>builder()
                    .success(Boolean.TRUE)
                    .value(new StringInputStream(logs))
                    .build()
            );
        } catch (UnsupportedEncodingException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
