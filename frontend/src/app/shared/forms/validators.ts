import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Mirrors Jakarta `@NotBlank`. */
export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value: unknown = control.value;
  return typeof value === 'string' && value.trim().length === 0 ? { notBlank: true } : null;
};

/** Rejects decimals for backend `Integer` fields. */
export const integer: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value: unknown = control.value;
  return typeof value === 'number' && !Number.isInteger(value) ? { integer: true } : null;
};
