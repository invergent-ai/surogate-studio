package net.statemesh.helm;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringJUnitConfig
public class HelmTest {
    String SMID = "qiw0iiosay";
    String KUBECONFIG = "apiVersion: v1\n" +
        "clusters:\n" +
        "- cluster:\n" +
        "    certificate-authority-data: LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUJkekNDQVIyZ0F3SUJBZ0lCQURBS0JnZ3Foa2pPUFFRREFqQWpNU0V3SHdZRFZRUUREQmhyTTNNdGMyVnkKZG1WeUxXTmhRREUzTXpFNE5UTTVOell3SGhjTk1qUXhNVEUzTVRRek1qVTJXaGNOTXpReE1URTFNVFF6TWpVMgpXakFqTVNFd0h3WURWUVFEREJock0zTXRjMlZ5ZG1WeUxXTmhRREUzTXpFNE5UTTVOell3V1RBVEJnY3Foa2pPClBRSUJCZ2dxaGtqT1BRTUJCd05DQUFSQjlDR2xwMjdZMnZLODA1STIyQnhBS0RUTWpleVlwWFgyK2FjTzh3VHEKNStCcWx2SkhSc3hwZzMvYmlwMWFYQ21ZYnVVcklKMEtyQ3oyUFczYS9LVXVvMEl3UURBT0JnTlZIUThCQWY4RQpCQU1DQXFRd0R3WURWUjBUQVFIL0JBVXdBd0VCL3pBZEJnTlZIUTRFRmdRVTlxbytFSVoyMjhBWmExeDUwd2h2CnFHYUF1UGt3Q2dZSUtvWkl6ajBFQXdJRFNBQXdSUUloQU1jMlVmdi9iZjROWWJ5WVFjdDRNT3ptNEx4dU1zMkkKZVVCVHkrcHc3Z3RsQWlCUnVVb29Zd3VaY0Q3d0pnekd3dTIwQ0xWZDZGUVZpTXlpZ0k3YzFnZWUxZz09Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K\n" +
        "    server: https://49.12.234.91:6443\n" +
        "  name: default\n" +
        "contexts:\n" +
        "- context:\n" +
        "    cluster: default\n" +
        "    user: default\n" +
        "  name: default\n" +
        "current-context: default\n" +
        "kind: Config\n" +
        "preferences: {}\n" +
        "users:\n" +
        "- name: default\n" +
        "  user:\n" +
        "    client-certificate-data: LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUJrakNDQVRlZ0F3SUJBZ0lJRHZKWTBoSFdlaXN3Q2dZSUtvWkl6ajBFQXdJd0l6RWhNQjhHQTFVRUF3d1kKYXpOekxXTnNhV1Z1ZEMxallVQXhOek14T0RVek9UYzJNQjRYRFRJME1URXhOekUwTXpJMU5sb1hEVEkxTVRFeApOekUwTXpJMU5sb3dNREVYTUJVR0ExVUVDaE1PYzNsemRHVnRPbTFoYzNSbGNuTXhGVEFUQmdOVkJBTVRESE41CmMzUmxiVHBoWkcxcGJqQlpNQk1HQnlxR1NNNDlBZ0VHQ0NxR1NNNDlBd0VIQTBJQUJDYzIwdjR3MkJDcHVMVjUKaHBIWlpVVTJLYUwrNUNYU2NRQncwbEE5dk12cExHcXBHWkJUR1hpY3Nwdk1hdncxZVVHYlRkVGRVaVYvcFZBNQpGYk5IRlIralNEQkdNQTRHQTFVZER3RUIvd1FFQXdJRm9EQVRCZ05WSFNVRUREQUtCZ2dyQmdFRkJRY0RBakFmCkJnTlZIU01FR0RBV2dCU2NjN1BqZmNwbmhFNlFJOVB1WUNoMytMVDI1ekFLQmdncWhrak9QUVFEQWdOSkFEQkcKQWlFQThvVDY3bVZJWnRZZ2JYZERocm93czJRR1NlWnkxaVIwRHcyeGJKclJ6QTRDSVFDamhsWFpIaXkwYmZjKwozL3JnS2xzdVFobytsR3VPM1ZJVFFIUlNHUWMvYlE9PQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCi0tLS0tQkVHSU4gQ0VSVElGSUNBVEUtLS0tLQpNSUlCZGpDQ0FSMmdBd0lCQWdJQkFEQUtCZ2dxaGtqT1BRUURBakFqTVNFd0h3WURWUVFEREJock0zTXRZMnhwClpXNTBMV05oUURFM016RTROVE01TnpZd0hoY05NalF4TVRFM01UUXpNalUyV2hjTk16UXhNVEUxTVRRek1qVTIKV2pBak1TRXdId1lEVlFRRERCaHJNM010WTJ4cFpXNTBMV05oUURFM016RTROVE01TnpZd1dUQVRCZ2NxaGtqTwpQUUlCQmdncWhrak9QUU1CQndOQ0FBUzJWWS9Ob0V6VmRlTForOHlZVWZEOUdaQ3VRRWh4TVR6L05hNk5RS0xPCjJsQnN4ZHBYUTJGMk9pWUJLLzIvMzdmaDVaMWxtUFk0cmJ2dC9IZGdPRm02bzBJd1FEQU9CZ05WSFE4QkFmOEUKQkFNQ0FxUXdEd1lEVlIwVEFRSC9CQVV3QXdFQi96QWRCZ05WSFE0RUZnUVVuSE96NDMzS1o0Uk9rQ1BUN21BbwpkL2kwOXVjd0NnWUlLb1pJemowRUF3SURSd0F3UkFJZ2VVYk1DVU92Vnp1cnM4WHd2VXN2bkRGanN5SzhQREZKCkFsaUVWUm94YlE4Q0lBaUlJQnJzVmNvL1VwUzlidm1YWmVMUEVXd2Z2WGFKQXVrdi8yY3ZsbGw2Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K\n" +
        "    client-key-data: LS0tLS1CRUdJTiBFQyBQUklWQVRFIEtFWS0tLS0tCk1IY0NBUUVFSUYxUTgzNzFicHFSU2gxSW91K2NlMUZtWVVLMjd0dmY0MmlJMS9jRzREbjdvQW9HQ0NxR1NNNDkKQXdFSG9VUURRZ0FFSnpiUy9qRFlFS200dFhtR2tkbGxSVFlwb3Y3a0pkSnhBSERTVUQyOHkra3NhcWtaa0ZNWgplSnl5bTh4cS9EVjVRWnROMU4xU0pYK2xVRGtWczBjVkh3PT0KLS0tLS1FTkQgRUMgUFJJVkFURSBLRVktLS0tLQo=";

    @Test
    void testInstallNodeApp() {
        try {
            var tmpKubeConfig = createTmpKubeConfig(KUBECONFIG);

            try {
                ProcessBuilder processBuilder = new ProcessBuilder(buildHelmInstallCommand());
                processBuilder.redirectErrorStream(true);
                try {
                    Process process = processBuilder.start();
                    int exitCode = process.waitFor();
                    String output = new String(process.getInputStream().readAllBytes());
                    System.out.println("Return code: " + exitCode);
                    System.out.println("Output: " + output);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                if (tmpKubeConfig != null) {
                    try {
                        Files.deleteIfExists(tmpKubeConfig);
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void uninstallNodeApp() {
        ProcessBuilder processBuilder = new ProcessBuilder(buildHelmUninstallCommand());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            String output = new String(process.getInputStream().readAllBytes());
            System.out.println("Return code: " + exitCode);
            System.out.println("Output: " + output);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    Path createTmpKubeConfig(String kubeConfig) throws IOException  {
        var tmpFile = Files.createTempFile("kcfg", ".yml");
        Files.writeString(tmpFile, kubeConfig);
        return tmpFile;
    }

    String[] buildHelmInstallCommand() {
        return new String[] {
            "helm",
            "install",
            "statemesh/nodeapp",
            "--kubeconfig",
            "/work/statemesh/k3s-master.yml",
            "--namespace",
            "kube-system",
            "--repository-config",
            "/work/statemesh/statemesh-console/src/main/docker/jib/repositories.yaml",
            "--set",
            "smid=qiw0iiosay",
            "--set",
            "chainId=11343",
            "--set",
            "rpcUrl=https://rpc-test.statemesh.net"
        };
    }

    String[] buildHelmUninstallCommand() {
        return new String[] {
            "helm",
            "uninstall",
            "nodeapp-"+SMID,
            "--kubeconfig",
            "/work/statemesh/k3s-master.yml",
            "--namespace",
            "kube-system"
        };
    }
}
