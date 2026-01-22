import {AbstractControl, FormArray, FormGroup, ValidationErrors, ValidatorFn} from '@angular/forms';
import {toPairs} from "lodash";

export const revalidateFormField = (formGroup: FormGroup, fieldName: string) => {
    const control = formGroup.get(fieldName);
    if (control) {
        control.markAsDirty();
        control.updateValueAndValidity();
    }
}

export const revalidateForm = (formGroup: FormGroup) => {
    for (const key in formGroup.controls) {
        formGroup.controls[key].markAsDirty();
        formGroup.controls[key].updateValueAndValidity();
    }
}

export const debugForm = (form: FormGroup) => {
    for (const field in form.controls) {
        const control: AbstractControl = form.controls[field];
        const errors = toPairs(control.errors as ValidationErrors);
        console.log(`${field}: ${control.status}: ${JSON.stringify(control.value)}, ${errors}`);
    }
}

export const markFormGroupTouched = (formGroup: FormGroup) => {
  Object.values(formGroup.controls).forEach(control => {
    control.markAsTouched();
    if (control instanceof FormArray) {
      control.controls.forEach(c => c.markAsTouched());
    }
    if (control instanceof FormGroup) {
      markFormGroupTouched(control);
    }
  });
}

export const ipValidator = (): ValidatorFn => {
  return (control: AbstractControl) => {
    const ipv4Regex = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/;
    const cidrv4Regex = /^(3[0-2]|[12]?[0-9])$/;

    if (!control.value) {
      return null;
    }
    if (ipv4Regex.test(control.value)) { // IP
      return null;
    }
    const split = control.value.split('/');
    if (split.length == 2 && ipv4Regex.test(split[0]) && cidrv4Regex.test(split[1])) { // CIDR
      return null;
    }

    return { ipError: true };
  };
}

export const dummyText = (length: number): string => {
  let result = '';
  const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
  const charactersLength = characters.length;
  let counter = 0;
  while (counter < length) {
    result += characters.charAt(Math.floor(Math.random() * charactersLength));
    counter += 1;
  }
  return result;
}
