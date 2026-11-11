import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { IconComponent, IconName } from '../../../../shared/components/icon/icon.component';

/** Reusable business-partner field group (name + optional country). */
@Component({
  selector: 'app-party-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, IconComponent],
  template: `
    <div class="party" [formGroup]="group()">
      <div class="party__head">
        <app-icon [name]="icon()" [size]="16" />
        <span class="party__label">{{ label() }}</span>
        <span class="req" aria-hidden="true">*</span>
      </div>

      <div class="party__grid">
        <div class="field">
          <label class="field__label sr-only" [attr.for]="id() + '-name'">{{ label() }} name</label>
          <input
            [id]="id() + '-name'"
            class="control"
            type="text"
            formControlName="name"
            placeholder="Company name"
            autocomplete="organization"
            [attr.aria-invalid]="invalid('name')"
          />
          @if (invalid('name')) {
            <span class="field__error" role="alert">{{ label() }} name is required.</span>
          }
        </div>

        <div class="field">
          <label class="field__label sr-only" [attr.for]="id() + '-country'">
            {{ label() }} country
          </label>
          <input
            [id]="id() + '-country'"
            class="control"
            type="text"
            formControlName="country"
            placeholder="Country"
            autocomplete="country-name"
          />
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .party {
        display: flex;
        flex-direction: column;
        gap: var(--space-2);
        padding: var(--space-3);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-sm);
        background: var(--color-canvas);
        transition: border-color var(--duration-fast) var(--ease-standard);
      }

      .party:focus-within {
        border-color: var(--color-accent);
        background: var(--color-surface);
      }

      .party__head {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        color: var(--color-ink-soft);
      }

      .party__head app-icon {
        color: var(--color-muted);
      }

      .party__label {
        font-size: 13px;
        font-weight: 600;
      }

      .req {
        color: var(--color-danger);
      }

      .party__grid {
        display: grid;
        grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
        gap: var(--space-2);
      }
    `,
  ],
})
export class PartyFieldComponent {
  readonly group = input.required<FormGroup>();
  readonly label = input.required<string>();
  readonly icon = input<IconName>('building');
  readonly id = input.required<string>();

  protected invalid(controlName: string): boolean {
    const control = this.group().get(controlName);
    return !!control && control.invalid && control.touched;
  }
}
