import {Injectable} from '@angular/core';
import {FormArray, FormControl, FormGroup, Validators} from '@angular/forms';
import {IDatabase, NewDatabase} from "../../model/database.model";
import {IVolumeMount} from "../../model/volume-mount.model";
import {IVolume} from "../../model/volume.model";
import {DEFAULT_VALUES} from "../../model/container.model";
import {NewFirewallEntry} from "../../model/firewall-entry.model";
import {FirewallLevel} from "../../model/enum/firewall-level.model";
import {PolicyType} from "../../model/enum/policy-type.model";
import {RuleType} from "../../model/enum/rule-type.model";
import {ipValidator} from "../../util/form.util";

type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };
type DatabaseFormGroupInput = IDatabase | PartialWithRequiredKeyOf<NewDatabase>;

type DatabaseFormGroupContent = {
  id: FormControl<IDatabase['id'] | NewDatabase['id']>;
  name: FormControl<IDatabase['name']>;
  internalName: FormControl<IDatabase['internalName']>;
  project: FormControl<IDatabase['project']>;
  description: FormControl<IDatabase['description']>;
  replicas: FormControl<number>;
  hasIngress: FormControl<boolean>;
  cpuLimit?: FormControl<number | null>;
  memLimit?: FormControl<string | null>;
  volumeMounts?: FormArray<FormGroup>;
  firewallEntries?: FormArray<FormGroup>;
};

export type DatabaseFormGroup = FormGroup<DatabaseFormGroupContent>;

@Injectable({providedIn: 'root'})
export class DatabaseFormService {

  createDatabaseForm(database: DatabaseFormGroupInput = {id: null}): DatabaseFormGroup {
    return new FormGroup<DatabaseFormGroupContent>({
      id: new FormControl<string | null>(database.id),
      name: new FormControl('', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]),
      internalName: new FormControl(''),
      project: new FormControl(null, [Validators.required]),
      description: new FormControl(null, [Validators.maxLength(250)]),
      replicas: new FormControl(1, [Validators.required, Validators.min(1), Validators.max(16)]),
      hasIngress: new FormControl(false, []),
      cpuLimit: new FormControl<number | null>(DEFAULT_VALUES.CPU.limit, [Validators.required, Validators.min(0.001), Validators.max(100)]),
      memLimit: new FormControl<string | null>(DEFAULT_VALUES.MEMORY.limit, [Validators.required, Validators.min(256), Validators.max(262144)]),
      volumeMounts: new FormArray<FormGroup>([]),
      firewallEntries: new FormArray<FormGroup>([])
    });
  }

  createVolumeMountForm(volumeMount?: Partial<IVolumeMount>): FormGroup {
    return new FormGroup({
      id: new FormControl<string | null>(volumeMount?.id ?? null),
      containerPath: new FormControl<string>(volumeMount?.containerPath ?? '', []),
      readOnly: new FormControl<boolean>(volumeMount?.readOnly ?? false),
      volume: new FormControl<IVolume>(volumeMount?.volume ?? null),
      volumeId: new FormControl<string | null>(volumeMount?.volume?.id ?? null)
    });
  }

  createFirewallEntryForm(firewallEntry: Partial<NewFirewallEntry> = {}): FormGroup {
    return new FormGroup({
      id: new FormControl(firewallEntry.id || null),
      cidr: new FormControl(firewallEntry.cidr, [Validators.required, ipValidator()]),
      level: new FormControl(firewallEntry.level || FirewallLevel.INGRESS, []),
      policy: new FormControl(firewallEntry.policy || PolicyType.INGRESS, []),
      rule: new FormControl(firewallEntry.rule || RuleType.ALLOW, [])
    });
  }
}
