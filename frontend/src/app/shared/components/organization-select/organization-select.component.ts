import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  computed,
  forwardRef,
  inject,
  input,
  signal,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Organization, OrganizationRole } from '../../../core/models/commercial.model';
import { ORGANIZATION_SERVICE } from '../../../core/services/organization.service';
import { NotificationService } from '../../../core/services/notification.service';
import { trimmedRequired } from '../../../core/utils/validators';
import { CountrySelectComponent } from '../country-select/country-select.component';
import { IconComponent } from '../icon/icon.component';

/**
 * Searchable company picker bound as a reactive-forms control (stores the
 * organization id or `null`). Optionally filters by role, offers a "None"
 * choice and can create a company inline without leaving the form.
 */
@Component({
  selector: 'app-organization-select',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, IconComponent, CountrySelectComponent],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OrganizationSelectComponent),
      multi: true,
    },
  ],
  templateUrl: './organization-select.component.html',
  styleUrl: './organization-select.component.scss',
})
export class OrganizationSelectComponent implements ControlValueAccessor {
  private readonly organizations = inject(ORGANIZATION_SERVICE);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  readonly role = input<OrganizationRole | null>(null);
  readonly allowEmpty = input<boolean>(true);
  readonly allowCreate = input<boolean>(false);
  readonly placeholder = input<string>('Select a company…');
  readonly controlId = input<string>('');
  readonly invalid = input<boolean>(false);

  protected readonly options = signal<Organization[]>([]);
  protected readonly value = signal<string | null>(null);
  protected readonly disabled = signal(false);
  protected readonly open = signal(false);
  protected readonly search = signal('');
  protected readonly creating = signal(false);
  protected readonly submitting = signal(false);
  protected readonly createError = signal<string | null>(null);

  protected readonly createForm = this.fb.group({
    legalName: ['', [trimmedRequired, Validators.maxLength(250)]],
    countryCode: [null as string | null],
  });

  protected readonly filtered = computed(() => {
    const needle = this.search().trim().toLowerCase();
    const items = this.options();
    if (!needle) {
      return items;
    }
    return items.filter((organization) => organization.legalName.toLowerCase().includes(needle));
  });

  protected readonly selectedName = computed(() => {
    const id = this.value();
    if (!id) {
      return '';
    }
    return this.options().find((organization) => organization.id === id)?.legalName ?? '—';
  });

  private propagate: (value: string | null) => void = () => {};
  protected onTouched: () => void = () => {};

  constructor() {
    this.load();
  }

  private load(): void {
    this.organizations
      .list({ size: 100, role: this.role() ?? undefined })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.options.set(page.items);
          this.ensureSelectedLoaded();
        },
        error: () => this.notifications.error('Unable to load companies.'),
      });
  }

  /** Keeps a historical (possibly inactive) selection readable. */
  private ensureSelectedLoaded(): void {
    const id = this.value();
    if (!id || this.options().some((organization) => organization.id === id)) {
      return;
    }
    this.organizations
      .get(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (organization) => this.options.update((items) => [...items, organization]),
        error: () => {},
      });
  }

  protected toggle(): void {
    if (this.disabled()) {
      return;
    }
    this.open.update((open) => !open);
  }

  protected close(): void {
    this.open.set(false);
    this.creating.set(false);
    this.search.set('');
    this.onTouched();
  }

  protected onSearch(value: string): void {
    this.search.set(value);
  }

  protected select(organization: Organization | null): void {
    const next = organization?.id ?? null;
    this.value.set(next);
    this.propagate(next);
    this.close();
  }

  protected startCreate(): void {
    this.createForm.reset({ legalName: '', countryCode: null });
    this.createError.set(null);
    this.creating.set(true);
  }

  protected cancelCreate(): void {
    this.creating.set(false);
    this.createError.set(null);
  }

  protected submitCreate(): void {
    if (this.createForm.invalid || this.submitting()) {
      this.createForm.markAllAsTouched();
      return;
    }
    const role = this.role();
    const raw = this.createForm.getRawValue();
    this.submitting.set(true);
    this.organizations
      .create({
        legalName: (raw.legalName ?? '').trim(),
        countryCode: raw.countryCode,
        roles: role ? [role] : [],
        active: true,
      })
      .subscribe({
        next: (organization) => {
          this.submitting.set(false);
          this.options.update((items) => [organization, ...items]);
          this.notifications.success(`Company ${organization.legalName} created.`);
          this.select(organization);
        },
        error: () => {
          this.submitting.set(false);
          this.createError.set('The company could not be created.');
        },
      });
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  writeValue(value: string | null): void {
    this.value.set(value ?? null);
    this.ensureSelectedLoaded();
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
