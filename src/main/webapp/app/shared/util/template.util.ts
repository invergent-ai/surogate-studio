import {WorkloadType} from "../model/enum/workload-type.model";
import {IContainer} from "../model/container.model";
import {IPort} from "../model/port.model";
import {IEnvironmentVariable} from "../model/environment-variable.model";
import {IAnnotation} from "../model/annotation.model";
import {IApplication} from "../model/application.model";
import {ApplicationStatus} from "../model/enum/application-status.model";
import {IVolumeMount} from "../model/volume-mount.model";

export const getTemplate = (application: IApplication): string => {
  if (!application) {
    return '';
  }

  const app = JSON.parse(JSON.stringify(application)) as IApplication;
  app.id = null;
  app.internalName = null;
  app.deployedNamespace = null;
  app.ingressHostName = null;
  app.status = ApplicationStatus.CREATED;
  app.workloadType  = WorkloadType.DEPLOYMENT;
  app.project = null;
  app.yamlConfig = null;
  if (app.containers) {
    app.containers.forEach((container: IContainer) => {
      if (container.volumeMounts) {
        (container.volumeMounts as IVolumeMount[]).forEach((volumeMount: IVolumeMount) => {
          volumeMount.id = null;
          if (volumeMount.volume) {
            volumeMount.volume.id = null;
            volumeMount.volume.project = null;
          }
        });
      }
      if (container.ports) {
        container.ports.forEach((port: IPort) => {
          port.id = null;
        });
      }
      if (container.envVars) {
        container.envVars.forEach((evar: IEnvironmentVariable) => {
          evar.id = null;
        });
      }

      container.id = null;
    });
  }
  if (app.annotations) {
    app.annotations.forEach((annotation: IAnnotation) => {
      annotation.id = null;
    });
  }

  return JSON.stringify(app, null, 4);
}
