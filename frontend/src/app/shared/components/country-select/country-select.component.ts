import { ChangeDetectionStrategy, Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { COUNTRY_CODES, countryName } from '../../../core/data/countries';

/**
 * ISO 3166-1 alpha-2 country picker bound as a reactive-forms control. Stores
 * the uppercase code or `null` when cleared.
 */
@Component({
  selector: 'app-country-select',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CountrySelectComponent),
      multi: true,
    },
  ],
  template: `
    <select
      class="control"
      [id]="id()"
      [value]="value() ?? ''"
      [disabled]="disabled()"
      (change)="select($any($event.target).value)"
      (blur)="onTouched()"
    >
      <option value="">{{ emptyLabel() }}</option>
      @for (code of countries; track code) {
        <option [value]="code">{{ name(code) }} ({{ code }})</option>
      }
    </select>
  `,
})
export class CountrySelectComponent implements ControlValueAccessor {
  readonly id = input<string>('');
  readonly emptyLabel = input<string>('None');

  protected readonly countries = COUNTRY_CODES;
  protected readonly value = signal<string | null>(null);
  protected readonly disabled = signal(false);

  private propagate: (value: string | null) => void = () => {};
  protected onTouched: () => void = () => {};

  protected name(code: string): string {
    return countryName(code);
  }

  protected select(raw: string): void {
    const next = raw ? raw.toUpperCase() : null;
    this.value.set(next);
    this.propagate(next);
  }

  writeValue(value: string | null): void {
    this.value.set(value ?? null);
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.propagate = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }
}
