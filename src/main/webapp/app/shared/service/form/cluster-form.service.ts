import {Injectable} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ICluster, NewCluster} from '../../model/cluster.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ICluster for edit and NewClusterFormGroupInput for create.
 */
type ClusterFormGroupInput = ICluster | PartialWithRequiredKeyOf<NewCluster>;

type ClusterFormDefaults = Pick<NewCluster, 'id'>;

type ClusterFormGroupContent = {
  id: FormControl<ICluster['id'] | NewCluster['id']>;
  name: FormControl<ICluster['name']>;
  cid: FormControl<ICluster['cid']>;
  kubeConfig: FormControl<ICluster['kubeConfig']>;
  openCostUrl: FormControl<ICluster['openCostUrl']>;
  prometheusUrl: FormControl<ICluster['prometheusUrl']>;
  redisUrl: FormControl<ICluster['redisUrl']>;
  masterIp: FormControl<ICluster['masterIp']>;
  publicIp: FormControl<ICluster['publicIp']>;
  vpnAuth: FormControl<ICluster['vpnAuth']>;
  requestVsLimitsCoefficientCpu: FormControl<ICluster['requestVsLimitsCoefficientCpu']>;
  requestVsLimitsCoefficientMemory: FormControl<ICluster['requestVsLimitsCoefficientMemory']>;
  description: FormControl<ICluster['description']>;
  zone: FormControl<ICluster['zone']>;
};

export type ClusterFormGroup = FormGroup<ClusterFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ClusterFormService {
  createClusterFormGroup(cluster: ClusterFormGroupInput = { id: null }): ClusterFormGroup {
    const clusterRawValue = {
      ...this.getFormDefaults(),
      ...cluster,
    };
    return new FormGroup<ClusterFormGroupContent>({
      id: new FormControl(
        { value: clusterRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(clusterRawValue.name, {
        validators: [Validators.required, Validators.maxLength(50)],
      }),
      cid: new FormControl(clusterRawValue.cid, {
        validators: [Validators.required],
      }),
      kubeConfig: new FormControl(clusterRawValue.kubeConfig),
      openCostUrl: new FormControl(clusterRawValue.openCostUrl),
      prometheusUrl: new FormControl(clusterRawValue.prometheusUrl),
      redisUrl: new FormControl(clusterRawValue.redisUrl),
      masterIp: new FormControl(clusterRawValue.masterIp),
      publicIp: new FormControl(clusterRawValue.publicIp),
      vpnAuth: new FormControl(clusterRawValue.vpnAuth),
      requestVsLimitsCoefficientCpu: new FormControl(clusterRawValue.requestVsLimitsCoefficientCpu),
      requestVsLimitsCoefficientMemory: new FormControl(clusterRawValue.requestVsLimitsCoefficientMemory),
      description: new FormControl(clusterRawValue.description, {
        validators: [Validators.maxLength(200)],
      }),
      zone: new FormControl(clusterRawValue.zone, {
        validators: [Validators.required],
      }),
    });
  }

  getCluster(form: ClusterFormGroup): ICluster | NewCluster {
    return form.getRawValue() as ICluster | NewCluster;
  }

  private getFormDefaults(): ClusterFormDefaults {
    return {
      id: null,
    };
  }
}
