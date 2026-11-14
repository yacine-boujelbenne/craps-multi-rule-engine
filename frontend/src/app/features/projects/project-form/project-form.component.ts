import { HttpClient } from '@angular/common/http';
import { APP_CONFIG } from '../../../core/config/app-config';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  CreateProjectRequest,
  isApiProblemError,
  NOTES_MAX_LENGTH,
  normalizeReference,
  Project,
  UpdateProjectRequest,
} from '../../../core/models/project.model';
import { OrganizationRole } from '../../../core/models/directory.model';
import { DirectoryStore } from '../../../core/services/directory.store';
import { NotificationService } from '../../../core/services/notification.service';
import { ProjectStore } from '../../../core/services/project.store';
import { suggestReference } from '../../../core/utils/format';
import { referencePattern, trimmedRequired } from '../../../core/utils/validators';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';

interface ProjectFormValue {
  reference: string;
  exporterId: string;
  producerIds: string[];
  consigneeId: string;
  notifyPartyId: string;
  freightForwarderId: string;
  shippingLineId: string;
  destinationCountryCode: string;
  portOfLoadingId: string;
  portOfDischargeId: string;
  expectedDepartureDate: string;
  notes: string;
}

@Component({
  selector: 'app-project-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, IconComponent, PageHeaderComponent, PanelComponent],
  templateUrl: './project-form.component.html',
  styleUrl: './project-form.component.scss',
})
export class ProjectFormComponent implements OnInit {
  private readonly http = inject(HttpClient);
  protected readonly savingPort = signal(false);
  protected addPort(name: string, countryCode: string): void {
    if (!name.trim() || !countryCode || this.savingPort()) return;
    this.savingPort.set(true);
    this.http.post(`${APP_CONFIG.apiBaseUrl}/ports`, { name: name.trim(), countryCode }).subscribe({
      next: () => {
        this.savingPort.set(false);
        this.directory.ensureLoaded();
        this.notifications.success('Port saved.');
      },
      error: () => {
        this.savingPort.set(false);
        this.notifications.error('Unable to save port.');
      },
    });
  }

  private readonly fb = inject(FormBuilder);
  private readonly store = inject(ProjectStore);
  private readonly directory = inject(DirectoryStore);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly formRef = viewChild<ElementRef<HTMLFormElement>>('projectForm');

  protected readonly submitting = signal(false);
  protected readonly loadingProject = signal(false);
  protected readonly notifySameAsConsignee = signal(false);
  protected readonly mode = signal<'create' | 'edit'>('create');
  protected readonly notesMaxLength = NOTES_MAX_LENGTH;
  protected readonly today = new Date().toISOString().slice(0, 10);

  private projectId: string | null = null;
  private expectedVersion = 0;

  protected readonly exporters = computed(() => this.directory.organizationsByRole('EXPORTER'));
  protected readonly producers = computed(() => this.directory.organizationsByRole('PRODUCER'));
  protected readonly consignees = computed(() => this.directory.organizationsByRole('CONSIGNEE'));
  protected readonly notifyParties = computed(() =>
    this.directory.organizationsByRole('NOTIFY_PARTY'),
  );
  protected readonly forwarders = computed(() =>
    this.directory.organizationsByRole('FREIGHT_FORWARDER'),
  );
  protected readonly shippingLines = computed(() =>
    this.directory.organizationsByRole('SHIPPING_LINE'),
  );
  protected readonly ports = this.directory.ports;
  protected readonly countries = this.directory.countries;

  protected readonly form = this.fb.group({
    reference: ['', [trimmedRequired, referencePattern]],
    exporterId: [''],
    producerIds: this.fb.control<string[]>([]),
    consigneeId: [''],
    notifyPartyId: [''],
    freightForwarderId: [''],
    shippingLineId: [''],
    destinationCountryCode: [''],
    portOfLoadingId: [''],
    portOfDischargeId: [''],
    expectedDepartureDate: [''],
    notes: ['', [Validators.maxLength(NOTES_MAX_LENGTH)]],
  });

  constructor() {
    effect(() => {
      if (this.mode() !== 'create') {
        return;
      }
      const reference = this.form.controls.reference;
      if (!reference.dirty) {
        reference.setValue(suggestReference(this.store.projects()), { emitEvent: false });
      }
    });

    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.projectId = id;
        this.mode.set('edit');
        this.loadProject(id);
      }
    });
  }

  ngOnInit(): void {
    this.directory.ensureLoaded();
    if (this.mode() === 'create') {
      this.store.loadPage({ page: 0, size: 100 });
    }
  }

  protected projectIdLink(): string {
    return this.projectId ? `/projects/${this.projectId}` : '/projects';
  }

  protected onNotifyToggle(checked: boolean): void {
    this.notifySameAsConsignee.set(checked);
    const notify = this.form.controls.notifyPartyId;
    if (checked) {
      notify.setValue(this.form.controls.consigneeId.value ?? '');
      notify.disable();
    } else {
      notify.enable();
    }
  }

  protected showError(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return !!control && control.invalid && (control.dirty || control.touched);
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
    if (this.mode() === 'edit' && this.projectId) {
      this.saveEdit(this.projectId);
    } else {
      this.create();
    }
  }

  private create(): void {
    const request = this.toCreateRequest();
    this.store.createProject(request).subscribe({
      next: (project) => {
        this.submitting.set(false);
        void this.router.navigate(['/projects', project.id]);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.handleError(error, 'The project could not be created. Please try again.');
      },
    });
  }

  private saveEdit(id: string): void {
    const patch: UpdateProjectRequest = {
      expectedVersion: this.expectedVersion,
      ...this.toCreateRequest(),
    };
    this.store.updateProject(id, patch).subscribe({
      next: (project) => {
        this.submitting.set(false);
        this.notifications.success(`Project ${project.reference} updated.`);
        void this.router.navigate(['/projects', project.id]);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        if (isApiProblemError(error) && error.status === 409) {
          this.notifications.error(
            'This project changed since you opened it. Reloading the latest version.',
          );
          this.loadProject(id);
          return;
        }
        this.handleError(error, 'The project could not be saved. Please try again.');
      },
    });
  }

  private loadProject(id: string): void {
    this.loadingProject.set(true);
    this.store.getProject(id).subscribe({
      next: (project) => {
        this.expectedVersion = project.version;
        this.patchForm(project);
        this.loadingProject.set(false);
      },
      error: () => {
        this.loadingProject.set(false);
        this.notifications.error('This project could not be loaded for editing.');
        void this.router.navigate(['/projects', id]);
      },
    });
  }

  private patchForm(project: Project): void {
    this.form.patchValue(
      {
        reference: project.reference,
        exporterId: project.exporterId ?? '',
        producerIds: project.producerIds,
        consigneeId: project.consigneeId ?? '',
        notifyPartyId: project.notifyPartyId ?? '',
        freightForwarderId: project.freightForwarderId ?? '',
        shippingLineId: project.shippingLineId ?? '',
        destinationCountryCode: project.destinationCountryCode ?? '',
        portOfLoadingId: project.portOfLoadingId ?? '',
        portOfDischargeId: project.portOfDischargeId ?? '',
        expectedDepartureDate: project.expectedDepartureDate ?? '',
        notes: project.notes ?? '',
      },
      { emitEvent: false },
    );
  }

  private toCreateRequest(): CreateProjectRequest {
    const value = this.form.getRawValue() as ProjectFormValue;
    const trimmed = (input: string): string | null => (input.trim() ? input.trim() : null);
    return {
      reference: normalizeReference(value.reference),
      exporterId: trimmed(value.exporterId),
      producerIds: value.producerIds ?? [],
      consigneeId: trimmed(value.consigneeId),
      notifyPartyId: trimmed(value.notifyPartyId),
      freightForwarderId: trimmed(value.freightForwarderId),
      shippingLineId: trimmed(value.shippingLineId),
      destinationCountryCode: trimmed(value.destinationCountryCode),
      portOfLoadingId: trimmed(value.portOfLoadingId),
      portOfDischargeId: trimmed(value.portOfDischargeId),
      expectedDepartureDate: value.expectedDepartureDate || null,
      notes: trimmed(value.notes),
    };
  }

  private handleError(error: unknown, fallback: string): void {
    if (isApiProblemError(error)) {
      let mapped = false;
      for (const fieldError of error.fieldErrors) {
        const control = this.form.get(fieldError.field);
        if (control) {
          control.setErrors({ server: fieldError.message });
          control.markAsTouched();
          mapped = true;
        }
      }
      this.notifications.error(error.problem.detail || error.problem.title || fallback);
      if (mapped) {
        this.focusFirstInvalid();
      }
      return;
    }
    this.notifications.error(fallback);
  }

  private focusFirstInvalid(): void {
    const host = this.formRef()?.nativeElement;
    const invalid = host?.querySelector<HTMLElement>(
      'input.ng-invalid, select.ng-invalid, textarea.ng-invalid',
    );
    invalid?.focus();
    invalid?.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
  }
}
