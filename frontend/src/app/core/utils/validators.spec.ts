import { FormControl, FormGroup } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import {
  countryCodePattern,
  matchesField,
  passwordPolicy,
  referencePattern,
  showsError,
  trimmedRequired,
} from './validators';

describe('validators', () => {
  it('treats whitespace-only values as missing', () => {
    expect(trimmedRequired(new FormControl('   '))).toEqual({ required: true });
    expect(trimmedRequired(new FormControl('EXP-2026-0001'))).toBeNull();
    expect(trimmedRequired(new FormControl(null))).toEqual({ required: true });
  });

  it('accepts contract reference formats', () => {
    expect(new FormControl('EXP-2026-0001', referencePattern).hasError('pattern')).toBe(false);
    expect(new FormControl('has spaces', referencePattern).hasError('pattern')).toBe(true);
    expect(new FormControl('-leading', referencePattern).hasError('pattern')).toBe(true);
  });

  it('accepts ISO alpha-2 country codes only', () => {
    expect(new FormControl('ES', countryCodePattern).hasError('pattern')).toBe(false);
    expect(new FormControl('esp', countryCodePattern).hasError('pattern')).toBe(true);
  });

  it('only surfaces errors once touched', () => {
    const control = new FormControl('', trimmedRequired);
    expect(showsError(control)).toBe(false);
    control.markAsTouched();
    expect(showsError(control)).toBe(true);
  });

  it('enforces the password length policy', () => {
    expect(passwordPolicy(new FormControl('short'))).toHaveProperty('passwordLength');
    expect(passwordPolicy(new FormControl(''))).toBeNull();
    expect(passwordPolicy(new FormControl('Long-Enough-Pass1'))).toBeNull();
    expect(passwordPolicy(new FormControl('x'.repeat(65)))).toHaveProperty('passwordLength');
  });

  it('rejects passwords above the bcrypt byte bound', () => {
    expect(passwordPolicy(new FormControl('é'.repeat(40)))).toHaveProperty('passwordBytes');
  });

  it('requires a confirmation field to match', () => {
    const group = new FormGroup({
      password: new FormControl('Long-Enough-Pass1'),
      confirm: new FormControl('different-value', matchesField('password')),
    });
    group.controls.confirm.updateValueAndValidity();
    expect(group.controls.confirm.hasError('mismatch')).toBe(true);
    group.controls.confirm.setValue('Long-Enough-Pass1');
    expect(group.controls.confirm.hasError('mismatch')).toBe(false);
  });
});
