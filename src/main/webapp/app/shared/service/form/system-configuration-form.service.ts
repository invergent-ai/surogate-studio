import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { ISystemConfiguration, NewSystemConfiguration } from '../../model/system-configuration.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ISystemConfiguration for edit and NewSystemConfigurationFormGroupInput for create.
 */
type SystemConfigurationFormGroupInput = ISystemConfiguration | PartialWithRequiredKeyOf<NewSystemConfiguration>;

type SystemConfigurationFormDefaults = Pick<NewSystemConfiguration, 'id'>;

type SystemConfigurationFormGroupContent = {
  id: FormControl<ISystemConfiguration['id'] | NewSystemConfiguration['id']>;
  webDomain: FormControl<ISystemConfiguration['webDomain']>;
};

export type SystemConfigurationFormGroup = FormGroup<SystemConfigurationFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class SystemConfigurationFormService {
  createSystemConfigurationFormGroup(systemConfiguration: SystemConfigurationFormGroupInput = { id: null }): SystemConfigurationFormGroup {
    const systemConfigurationRawValue = {
      ...this.getFormDefaults(),
      ...systemConfiguration,
    };
    return new FormGroup<SystemConfigurationFormGroupContent>({
      id: new FormControl(
        { value: systemConfigurationRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      webDomain: new FormControl(systemConfigurationRawValue.webDomain),
    });
  }

  getSystemConfiguration(form: SystemConfigurationFormGroup): ISystemConfiguration | NewSystemConfiguration {
    return form.getRawValue() as ISystemConfiguration | NewSystemConfiguration;
  }

  private getFormDefaults(): SystemConfigurationFormDefaults {
    return {
      id: null,
    };
  }
}
