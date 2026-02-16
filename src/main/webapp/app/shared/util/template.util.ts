import {WorkloadType} from "../model/enum/workload-type.model";
import {IContainer} from "../model/container.model";
import {IPort} from "../model/port.model";
import {IEnvironmentVariable} from "../model/environment-variable.model";
import {IAnnotation} from "../model/annotation.model";
import {IApplication} from "../model/application.model";
import {ApplicationStatus} from "../model/enum/application-status.model";
import {IVolumeMount} from "../model/volume-mount.model";
import { IAppTemplate } from '../model/app-template.model';
import { dummyText } from './form.util';
import { lastValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { IProject } from '../model/project.model';
import { MenuService } from '../../private/layout/service/app-menu.service';
import { ApplicationMode } from '../model/enum/application-mode.model';
import { ApplicationService } from '../service/application.service';

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

export const createAppFromTemplate =
  async (template: IAppTemplate, router: Router, menuService: MenuService,
         applicationService: ApplicationService, project: IProject) => {
  const app = JSON.parse(template.template!) as IApplication;
  app.project = project;
  app.containers.forEach((container: IContainer) => {
    if (container.ports) {
      container.ports.forEach(port => {
        if (!port.name.endsWith("-x")) {
          port.name = (dummyText(5).toLowerCase() + port.name).substring(0, 7);
        }
      });
    }
    if (container.volumeMounts) {
      container.volumeMounts.forEach(volumeMount => {
        if (volumeMount.volume) {
          volumeMount.volume.name += dummyText(5).toLowerCase();
          volumeMount.volume.project = project;
        }
      });
    }
  });
  app.fromTemplate = true;
  if (!app.mode) {
    app.mode = ApplicationMode.APPLICATION;
  }

  const created = await lastValueFrom(applicationService.save(app));
  await router.navigate([app.mode === ApplicationMode.MODEL ? 'models' : '/apps'], {
    queryParams: {
      id: created.id
    }
  });
  menuService.reload(app.project?.id);
}
