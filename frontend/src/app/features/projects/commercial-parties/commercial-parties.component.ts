import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { COMMERCIAL_PARTY_SLOTS, CommercialParties } from '../../../core/models/commercial.model';
import { isApiProblemError } from '../../../core/models/project.model';
import { COMMERCIAL_PARTIES_SERVICE } from '../../../core/services/commercial-parties.service';
import { NotificationService } from '../../../core/services/notification.service';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { OrganizationSelectComponent } from '../../../shared/components/organization-select/organization-select.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';

/**
 * Shipment commercial parties (supplier/client/importer/consignee). Editing is
 * limited to ADMIN or an assigned OPERATOR with the PROJECT_SETUP grant; other
 * viewers see a read-only snapshot. Reference-only drafts stay valid.
 */
@Component({
  selector: 'app-commercial-parties',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, IconComponent, OrganizationSelectComponent, PanelComponent],
  templateUrl: './commercial-parties.component.html',
  styleUrl: './commercial-parties.component.scss',
})
export class CommercialPartiesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly parties = inject(COMMERCIAL_PARTIES_SERVICE);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly projectId = input.required<string>();
  readonly canEdit = input<boolean>(false);
  readonly saved = output<void>();
  /** Emits the shared project version carried by the loaded parties record. */
  readonly loaded = output<number>();

  protected readonly slots = COMMERCIAL_PARTY_SLOTS;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly clientIsConsignee = signal(false);

  private version = 0;

  protected readonly form = this.fb.group({
    supplierId: [null as string | null],
    clientId: [null as string | null],
    importerId: [null as string | null],
    consigneeId: [null as string | null],
  });

  ngOnInit(): void {
    if (!this.canEdit()) {
      this.form.disable({ emitEvent: false });
    }

    // While mirrored, keep the consignee in sync with the client selection.
    this.form.controls.clientId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((clientId) => {
        if (this.clientIsConsignee()) {
          this.form.controls.consigneeId.setValue(clientId, { emitEvent: false });
        }
      });

    this.load();
  }

  protected onClientIsConsigneeChange(checked: boolean): void {
    this.clientIsConsignee.set(checked);
    const consignee = this.form.controls.consigneeId;
    if (checked) {
      consignee.setValue(this.form.controls.clientId.value);
      consignee.disable({ emitEvent: false });
    } else {
      consignee.enable({ emitEvent: false });
    }
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) {
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.parties
      .update(this.projectId(), {
        expectedVersion: this.version,
        supplierId: value.supplierId,
        clientId: value.clientId,
        importerId: value.importerId,
        consigneeId: value.consigneeId,
      })
      .subscribe({
        next: (result) => {
          this.saving.set(false);
          this.apply(result);
          this.notifications.success('Commercial parties saved.');
          this.saved.emit();
        },
        error: (error: unknown) => {
          this.saving.set(false);
          if (isApiProblemError(error)) {
            if (error.status === 409) {
              this.notifications.error(
                'This shipment changed since it was loaded. Reloading the latest parties.',
              );
              this.load();
              return;
            }
            for (const fieldError of error.fieldErrors) {
              const control = this.form.get(fieldError.field);
              if (control) {
                control.setErrors({ server: fieldError.message });
                control.markAsTouched();
              }
            }
            this.notifications.error(
              error.problem.detail || error.problem.title || 'Unable to save parties.',
            );
            return;
          }
          this.notifications.error('Unable to save commercial parties.');
        },
      });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.parties.get(this.projectId()).subscribe({
      next: (result) => {
        this.apply(result);
        this.loading.set(false);
        this.loaded.emit(result.version);
      },
      error: () => {
        this.error.set('Unable to load commercial parties.');
        this.loading.set(false);
      },
    });
  }

  protected slotError(key: string): string | null {
    const control = this.form.get(key);
    return control?.hasError('server') ? (control.getError('server') as string) : null;
  }

  private apply(result: CommercialParties): void {
    this.version = result.version;
    const mirrorConsignee = result.clientId !== null && result.clientId === result.consigneeId;
    this.clientIsConsignee.set(mirrorConsignee);
    for (const control of Object.values(this.form.controls)) {
      control.setErrors(null);
    }
    this.form.patchValue(
      {
        supplierId: result.supplierId,
        clientId: result.clientId,
        importerId: result.importerId,
        consigneeId: result.consigneeId,
      },
      { emitEvent: false },
    );
    if (this.canEdit()) {
      const consignee = this.form.controls.consigneeId;
      if (mirrorConsignee) {
        consignee.disable({ emitEvent: false });
      } else {
        consignee.enable({ emitEvent: false });
      }
    }
  }
}
