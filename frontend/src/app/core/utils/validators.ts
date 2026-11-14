import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import {
  PASSWORD_MAX_BYTES,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  utf8ByteLength,
} from '../models/account.model';
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

/**
 * Password policy from the accounts contract: 12–64 characters and at most 72
 * UTF-8 bytes (bcrypt bound). Passwords are never trimmed.
 */
export function passwordPolicy(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (typeof value !== 'string' || value.length === 0) {
    return null;
  }
  if (value.length < PASSWORD_MIN_LENGTH) {
    return { passwordLength: { requiredLength: PASSWORD_MIN_LENGTH } };
  }
  if (value.length > PASSWORD_MAX_LENGTH) {
    return { passwordLength: { maxLength: PASSWORD_MAX_LENGTH } };
  }
  if (utf8ByteLength(value) > PASSWORD_MAX_BYTES) {
    return { passwordBytes: { maxBytes: PASSWORD_MAX_BYTES } };
  }
  return null;
}

/** Requires the control value to equal the value of another control. */
export function matchesField(field: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const parent = control.parent;
    if (!parent) {
      return null;
    }
    const other = parent.get(field);
    return other && other.value !== control.value ? { mismatch: true } : null;
  };
}

/** Returns true when the control has the given error and is dirty or touched. */
export function showsError(control: AbstractControl | null, error = 'required'): boolean {
  return !!control && control.hasError(error) && (control.dirty || control.touched);
}
