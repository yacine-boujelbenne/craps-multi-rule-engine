import { FormControl } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { countryCodePattern, referencePattern, showsError, trimmedRequired } from './validators';

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
});
