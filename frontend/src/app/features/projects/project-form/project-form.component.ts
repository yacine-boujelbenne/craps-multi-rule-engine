import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CreateProjectRequest } from '../../../core/models/project.model';
import { NotificationService } from '../../../core/services/notification.service';
import { ProjectStore } from '../../../core/services/project.store';
import { suggestReference, todayIso } from '../../../core/utils/format';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';
import { PartyFieldComponent } from './party-field/party-field.component';

/** Rejects dates strictly before today (ETD is an estimate but must be forward-looking). */
function notInPast(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string | null;
  if (!value) {
    return null;
  }
  return value < todayIso() ? { pastDate: true } : null;
}

interface PartyFormValue {
  name: string;
  country: string;
}

interface ProjectFormValue {
  reference: string;
  exporter: PartyFormValue;
  producer: PartyFormValue;
  consignee: PartyFormValue;
  notifyParty: PartyFormValue;
  freightForwarder: PartyFormValue;
  shippingLine: PartyFormValue;
  destination: string;
  loadingPort: string;
  dischargePort: string;
  etd: string;
}

function normalizeParty(party: PartyFormValue): { name: string; country?: string } {
  return {
    name: party.name.trim(),
    country: party.country.trim() || undefined,
  };
}

@Component({
  selector: 'app-project-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    IconComponent,
    PageHeaderComponent,
    PanelComponent,
    PartyFieldComponent,
  ],
  templateUrl: './project-form.component.html',
  styleUrl: './project-form.component.scss',
})
export class ProjectFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(ProjectStore);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  private readonly formRef = viewChild<ElementRef<HTMLFormElement>>('projectForm');

  protected readonly submitting = signal(false);
  protected readonly notifySameAsConsignee = signal(false);
  protected readonly today = todayIso();

  protected readonly form = this.fb.group({
    reference: ['', [Validators.required, Validators.pattern(/^EXP-\d{4}-\d{4}$/)]],
    exporter: this.partyGroup(),
    producer: this.partyGroup(),
    consignee: this.partyGroup(),
    notifyParty: this.partyGroup(),
    freightForwarder: this.partyGroup(),
    shippingLine: this.partyGroup(),
    destination: ['', Validators.required],
    loadingPort: ['', Validators.required],
    dischargePort: ['', Validators.required],
    etd: ['', [Validators.required, notInPast]],
  });

  constructor() {
    effect(() => {
      const projects = this.store.projects();
      const reference = this.form.controls.reference;
      if (!reference.dirty) {
        reference.setValue(suggestReference(projects));
      }
    });
  }

  ngOnInit(): void {
    this.store.loadProjects();
  }

  protected party(
    name:
      'exporter' | 'producer' | 'consignee' | 'notifyParty' | 'freightForwarder' | 'shippingLine',
  ): FormGroup {
    return this.form.controls[name];
  }

  protected onNotifyToggle(checked: boolean): void {
    this.notifySameAsConsignee.set(checked);
    const notify = this.form.controls.notifyParty;
    if (checked) {
      notify.setValue(this.form.controls.consignee.getRawValue());
      notify.disable();
    } else {
      notify.enable();
    }
  }

  protected showError(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return !!control && control.invalid && control.touched;
  }

  protected submit(): void {
    if (this.submitting()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.focusFirstInvalid();
      this.notifications.error('Please correct the highlighted fields before saving.');
      return;
    }

    this.submitting.set(true);
    const request = this.toRequest();

    this.store.createProject(request).subscribe({
      next: (project) => {
        this.submitting.set(false);
        void this.router.navigate(['/projects', project.id]);
      },
      error: () => {
        this.submitting.set(false);
        this.notifications.error('The project could not be created. Please try again.');
      },
    });
  }

  private toRequest(): CreateProjectRequest {
    const value = this.form.getRawValue() as ProjectFormValue;
    return {
      reference: value.reference.trim(),
      exporter: normalizeParty(value.exporter),
      producer: normalizeParty(value.producer),
      consignee: normalizeParty(value.consignee),
      notifyParty: normalizeParty(value.notifyParty),
      freightForwarder: normalizeParty(value.freightForwarder),
      shippingLine: normalizeParty(value.shippingLine),
      destination: value.destination.trim(),
      loadingPort: value.loadingPort.trim(),
      dischargePort: value.dischargePort.trim(),
      etd: value.etd,
    };
  }

  private focusFirstInvalid(): void {
    const host = this.formRef()?.nativeElement;
    const invalid = host?.querySelector<HTMLElement>('input.ng-invalid, select.ng-invalid');
    invalid?.focus();
    invalid?.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
  }

  private partyGroup(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      country: [''],
    });
  }
}
