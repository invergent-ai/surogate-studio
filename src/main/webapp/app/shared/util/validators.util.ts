import {AbstractControl, ValidationErrors, ValidatorFn} from "@angular/forms";

export const sciNumberValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const v = (control.value ?? '').toString().trim();

  if (v === '') return null;

  const re = /^[+-]?(\d+(\.\d+)?|\.\d+)([eE][+-]?\d+)?$/;
  return re.test(v) ? null : { sciNumber: true };
};

export const filterSciNumber = (ev: InputEvent) => {
  if (ev.inputType !== 'insertText' || ev.data == null) return;

  // allow digits, sign, dot, exponent markers
  if (!/^[0-9eE+\-\.]$/.test(ev.data)) {
    ev.preventDefault();
  }
}

export const doubleValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const v = (control.value ?? '').toString().trim();

  if (v === '') return null; // allow empty if not required

  const re = /^(?:0|[1-9]\d*)(?:\.\d{1,4})?$/;
  return re.test(v) ? null : { double: true };
};

export const filterDouble = (ev: InputEvent) => {
  if (ev.inputType !== 'insertText' || ev.data == null) return;

  // only digits, dot
  if (!/^[0-9.]$/.test(ev.data)) {
    ev.preventDefault();
  }
}

export const jsonValidator = (control: AbstractControl): ValidationErrors | null => {
  if (!control.value) return null;

  try {
    JSON.parse(control.value);
    return null;
  } catch {
    return { invalidJson: true };
  }
}
