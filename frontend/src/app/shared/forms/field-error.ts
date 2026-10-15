import { AbstractControl } from '@angular/forms';

/** Returns the message for the first validation error of a control. */
export function fieldError(control: AbstractControl): string {
  const errors = control.errors;
  if (!errors) {
    return '';
  }
  if (errors['required'] || errors['notBlank']) {
    return 'This field is required';
  }
  if (errors['min']) {
    return `Must be at least ${(errors['min'] as { min: number }).min}`;
  }
  if (errors['max']) {
    return `Must be at most ${(errors['max'] as { max: number }).max}`;
  }
  if (errors['email']) {
    return 'Enter a valid email address';
  }
  if (errors['integer']) {
    return 'Must be a whole number';
  }
  if (errors['matDatepickerParse']) {
    return 'Enter a valid date';
  }
  return 'Invalid value';
}
