import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IZone, NewZone } from '../../model/zone.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IZone for edit and NewZoneFormGroupInput for create.
 */
type ZoneFormGroupInput = IZone | PartialWithRequiredKeyOf<NewZone>;

type ZoneFormDefaults = Pick<NewZone, 'id'>;

type ZoneFormGroupContent = {
  id: FormControl<IZone['id'] | NewZone['id']>;
  name: FormControl<IZone['name']>;
  zoneId: FormControl<IZone['zoneId']>;
  organization: FormControl<IZone['organization']>;
};

export type ZoneFormGroup = FormGroup<ZoneFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ZoneFormService {
  createZoneFormGroup(zone: ZoneFormGroupInput = { id: null }): ZoneFormGroup {
    const zoneRawValue = {
      ...this.getFormDefaults(),
      ...zone,
    };
    return new FormGroup<ZoneFormGroupContent>({
      id: new FormControl(
        { value: zoneRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(zoneRawValue.name, {
        validators: [Validators.required, Validators.maxLength(50)],
      }),
      zoneId: new FormControl(zoneRawValue.zoneId, {
        validators: [Validators.required],
      }),
      organization: new FormControl(zoneRawValue.organization, {
        validators: [Validators.required],
      }),
    });
  }

  getZone(form: ZoneFormGroup): IZone | NewZone {
    return form.getRawValue() as IZone | NewZone;
  }

  private getFormDefaults(): ZoneFormDefaults {
    return {
      id: null,
    };
  }
}
