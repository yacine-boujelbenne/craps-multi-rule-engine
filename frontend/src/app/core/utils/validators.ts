import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { COUNTRY_CODE_PATTERN, REFERENCE_PATTERN } from '../models/project.model';

/** Required validator that ignores whitespace-only values. */
export function trimmedRequired(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value === null || value === undefined) {
    return { required: true };
  }
  return typeof value === 'string' && value.trim().length === 0 ? { required: true } : null;
}

/** Contract reference format (trim before validating, store uppercase). */
export const referencePattern: ValidatorFn = Validators.pattern(REFERENCE_PATTERN);

/** ISO 3166-1 alpha-2 country code (uppercase). */
export const countryCodePattern: ValidatorFn = Validators.pattern(COUNTRY_CODE_PATTERN);

/** Returns true when the control has the given error and is dirty or touched. */
export function showsError(control: AbstractControl | null, error = 'required'): boolean {
  return !!control && control.hasError(error) && (control.dirty || control.touched);
}
