package net.statemesh.k8s.task.node;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.options.CreateOptions;
import net.statemesh.k8s.crd.helm.models.V1HelmChart;
import net.statemesh.k8s.crd.helm.models.V1HelmChartSpec;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.BaseTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.thread.VirtualWait;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.statemesh.k8s.util.K8SConstants.HELM_CHART_API_VERSION;
import static net.statemesh.k8s.util.K8SConstants.HELM_CHART_GROUP;

public class InstallHelmChartTask extends BaseMutationTask<Void> {
    private final String releaseName;
    private final String chartName;
    private final String version;
    private final String repoUrl;
    private final String authSecret;
    private final Map<String, Object> values;

    public InstallHelmChartTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String releaseName,
        String chartName,
        String version,
        String repoUrl,
        String authSecret,
        Map<String, Object> values
    ) {
        super(apiStub, taskConfig, namespace);
        this.releaseName = releaseName;
        this.chartName = chartName;
        this.version = version;
        this.repoUrl = repoUrl;
        this.authSecret = authSecret;
        this.values = values;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        var chartSpec = new V1HelmChartSpec()
            .chart(chartName)
            .targetNamespace(getNamespace())
            .repo(repoUrl)
            .set(values);

        if (version != null) {
            chartSpec.version(version);
        }

        if (authSecret != null) {
            chartSpec.authSecret(new V1LocalObjectReference().name(authSecret));
        }

        getApiStub().getHelmController().create(
            getNamespace(),
            new V1HelmChart()
                .apiVersion(HELM_CHART_GROUP + "/" + HELM_CHART_API_VERSION)
                .kind("HelmChart")
                .metadata(
                    new V1ObjectMeta()
                        .name(releaseName)
                        .namespace(getNamespace())
                )
                .spec(chartSpec),
            new CreateOptions()
        );
    }

    @Override
    protected boolean isReady() throws ApiException {
        return true;
    }
}
