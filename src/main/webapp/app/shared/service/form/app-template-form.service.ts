// app-template-form.service.ts
import {Injectable} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {AppTemplateExtra, IAppTemplate, NewAppTemplate} from "../../model/app-template.model";
import {jsonValidator} from "../../util/validators.util";

type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };
type AppTemplateFormGroupInput = IAppTemplate | PartialWithRequiredKeyOf<NewAppTemplate>;
type AppTemplateFormDefaults = Pick<NewAppTemplate, 'id'>;

type AppTemplateFormGroupContent = {
  id: FormControl<IAppTemplate['id'] | NewAppTemplate['id']>;
  name?: FormControl<IAppTemplate['name']>;
  description?: FormControl<IAppTemplate['description']>;
  longDescription?: FormControl<IAppTemplate['longDescription']>;
  template?: FormControl<IAppTemplate['template']>;
  extraConfig?: FormControl<IAppTemplate['template']>;
  category?: FormControl<IAppTemplate['category']>;
  icon?: FormControl<IAppTemplate['icon']>;
  zorder?: FormControl<IAppTemplate['zorder']>;
  provider?: FormControl<IAppTemplate['provider']>;  // Now ProviderDTO type
  hashtags?: FormControl<IAppTemplate['hashtags']>;
};

export type AppTemplateFormGroup = FormGroup<AppTemplateFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class AppTemplateFormService {
  createAppTemplateFormGroup(appTemplate: AppTemplateFormGroupInput = { id: null }): AppTemplateFormGroup {
    const appTemplateRawValue = {
      ...this.getFormDefaults(),
      ...appTemplate,
    };
    return new FormGroup<AppTemplateFormGroupContent>({
      id: new FormControl(
        { value: appTemplateRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(appTemplateRawValue.name, {
        validators: [Validators.required],
      }),
      description: new FormControl(appTemplateRawValue.description, {
        validators: [Validators.required],
      }),
      longDescription: new FormControl(appTemplateRawValue.longDescription, {
        validators: [Validators.required],
      }),
      template: new FormControl(appTemplateRawValue.template, {
        validators: [Validators.required, jsonValidator],
      }),
      extraConfig: new FormControl(null, {
        validators: [Validators.required, jsonValidator],
      }),
      category: new FormControl(appTemplateRawValue.category, {
        validators: [Validators.required],
      }),
      icon: new FormControl(appTemplateRawValue.icon, {
        validators: [],
      }),
      zorder: new FormControl(appTemplateRawValue.zorder, {
        validators: [],
      }),
      hashtags: new FormControl(appTemplateRawValue.hashtags, {
        validators: [],
      })
    });
  }

  getAppTemplate(form: AppTemplateFormGroup): AppTemplateExtra | NewAppTemplate {
    return form.getRawValue() as AppTemplateExtra |  NewAppTemplate;
  }

  private getFormDefaults(): AppTemplateFormDefaults {
    return {
      id: null,
    };
  }
}
