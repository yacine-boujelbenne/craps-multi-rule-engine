import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import {
  CARTON_NET_KG_PATTERN,
  CODE_MAX_LENGTH,
  CODE_PATTERN,
  CURRENCY_PATTERN,
  Grade,
  GradePage,
  HS_CODE_PATTERN,
  PRICE_PER_KG_PATTERN,
  PRODUCT_NAME_MAX_LENGTH,
  Product,
  ProductPage,
  SCIENTIFIC_NAME_MAX_LENGTH,
  GRADE_NAME_MAX_LENGTH,
  normalizeCode,
  normalizeCurrency,
  nullIfBlank,
} from '../../../core/models/commercial.model';
import { isApiProblemError } from '../../../core/models/project.model';
import { CATALOG_SERVICE } from '../../../core/services/catalog.service';
import { NotificationService } from '../../../core/services/notification.service';
import { trimmedRequired } from '../../../core/utils/validators';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

type View = 'list' | 'form' | 'grades';
type GradeMode = 'list' | 'form';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-product-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    IconComponent,
    PageHeaderComponent,
    PanelComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CATALOG_SERVICE);
  private readonly notifications = inject(NotificationService);

  protected readonly codeMaxLength = CODE_MAX_LENGTH;
  protected readonly productNameMaxLength = PRODUCT_NAME_MAX_LENGTH;
  protected readonly scientificNameMaxLength = SCIENTIFIC_NAME_MAX_LENGTH;
  protected readonly gradeNameMaxLength = GRADE_NAME_MAX_LENGTH;

  protected readonly view = signal<View>('list');
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly includeInactive = signal(false);
  protected readonly searchTerm = signal('');

  protected readonly page = signal<ProductPage>({
    items: [],
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  protected readonly selectedProduct = signal<Product | null>(null);

  protected readonly grades = signal<GradePage>({
    items: [],
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  protected readonly gradesLoading = signal(false);
  protected readonly gradesError = signal<string | null>(null);
  protected readonly gradeMode = signal<GradeMode>('list');
  protected readonly selectedGrade = signal<Grade | null>(null);
  protected readonly gradeSubmitting = signal(false);

  protected readonly rangeStart = computed(() =>
    this.page().totalElements === 0 ? 0 : this.page().page * this.page().size + 1,
  );
  protected readonly rangeEnd = computed(() =>
    Math.min((this.page().page + 1) * this.page().size, this.page().totalElements),
  );

  protected readonly productForm = this.fb.group({
    code: [
      '',
      [trimmedRequired, Validators.maxLength(CODE_MAX_LENGTH), Validators.pattern(CODE_PATTERN)],
    ],
    name: ['', [trimmedRequired, Validators.maxLength(PRODUCT_NAME_MAX_LENGTH)]],
    scientificName: ['', [Validators.maxLength(SCIENTIFIC_NAME_MAX_LENGTH)]],
    hsCode: ['', [Validators.pattern(HS_CODE_PATTERN)]],
    active: [true],
  });

  protected readonly gradeForm = this.fb.group(
    {
      code: [
        '',
        [trimmedRequired, Validators.maxLength(CODE_MAX_LENGTH), Validators.pattern(CODE_PATTERN)],
      ],
      name: ['', [trimmedRequired, Validators.maxLength(GRADE_NAME_MAX_LENGTH)]],
      displayOrder: [0, [Validators.min(0)]],
      defaultCartonNetKg: ['', [Validators.pattern(CARTON_NET_KG_PATTERN)]],
      defaultPricePerKg: ['', [Validators.pattern(PRICE_PER_KG_PATTERN)]],
      defaultCurrency: ['', [Validators.pattern(CURRENCY_PATTERN)]],
      active: [true],
    },
    { validators: priceCurrencyPairing },
  );

  private readonly search$ = new Subject<string>();

  constructor() {
    this.search$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((term) => {
        this.searchTerm.set(term);
        this.load(0);
      });
  }

  ngOnInit(): void {
    this.load(0);
  }

  // ---- Products -------------------------------------------------------------

  protected onSearch(value: string): void {
    this.search$.next(value);
  }

  protected onIncludeInactiveChange(checked: boolean): void {
    this.includeInactive.set(checked);
    this.load(0);
  }

  protected startCreateProduct(): void {
    this.selectedProduct.set(null);
    this.productForm.reset({
      code: '',
      name: '',
      scientificName: '',
      hsCode: '',
      active: true,
    });
    this.view.set('form');
  }

  protected startEditProduct(product: Product): void {
    this.selectedProduct.set(product);
    this.productForm.patchValue({
      code: product.code,
      name: product.name,
      scientificName: product.scientificName ?? '',
      hsCode: product.hsCode ?? '',
      active: product.active,
    });
    this.view.set('form');
  }

  protected submitProduct(): void {
    if (this.productForm.invalid || this.submitting()) {
      this.productForm.markAllAsTouched();
      return;
    }
    const value = this.productForm.getRawValue();
    const current = this.selectedProduct();
    const base = {
      code: normalizeCode(value.code ?? ''),
      name: (value.name ?? '').trim(),
      scientificName: nullIfBlank(value.scientificName),
      hsCode: nullIfBlank(value.hsCode),
    };
    this.submitting.set(true);
    const request$ = current
      ? this.catalog.updateProduct(current.id, {
          expectedVersion: current.version,
          ...base,
          active: value.active ?? true,
        })
      : this.catalog.createProduct({ ...base, active: true });

    request$.subscribe({
      next: (product) => {
        this.submitting.set(false);
        this.notifications.success(
          current ? `Product ${product.code} updated.` : `Product ${product.code} created.`,
        );
        this.backToProducts();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.handleError(error, this.productForm, 'The product could not be saved.');
      },
    });
  }

  protected backToProducts(): void {
    this.view.set('list');
    this.selectedProduct.set(null);
    this.load(this.page().page);
  }

  // ---- Grades ---------------------------------------------------------------

  protected openGrades(product: Product): void {
    this.selectedProduct.set(product);
    this.gradeMode.set('list');
    this.selectedGrade.set(null);
    this.view.set('grades');
    this.loadGrades(0);
  }

  protected startCreateGrade(): void {
    this.selectedGrade.set(null);
    this.gradeForm.reset({
      code: '',
      name: '',
      displayOrder: 0,
      defaultCartonNetKg: '',
      defaultPricePerKg: '',
      defaultCurrency: '',
      active: true,
    });
    this.gradeMode.set('form');
  }

  protected startEditGrade(grade: Grade): void {
    this.selectedGrade.set(grade);
    this.gradeForm.patchValue({
      code: grade.code,
      name: grade.name,
      displayOrder: grade.displayOrder,
      defaultCartonNetKg: grade.defaultCartonNetKg ?? '',
      defaultPricePerKg: grade.defaultPricePerKg ?? '',
      defaultCurrency: grade.defaultCurrency ?? '',
      active: grade.active,
    });
    this.gradeMode.set('form');
  }

  protected cancelGradeForm(): void {
    this.gradeMode.set('list');
    this.selectedGrade.set(null);
  }

  protected submitGrade(): void {
    const product = this.selectedProduct();
    if (!product || this.gradeForm.invalid || this.gradeSubmitting()) {
      this.gradeForm.markAllAsTouched();
      return;
    }
    const value = this.gradeForm.getRawValue();
    const current = this.selectedGrade();
    const price = nullIfBlank(value.defaultPricePerKg);
    const currency = price ? normalizeCurrency(value.defaultCurrency ?? '') : null;
    const base = {
      code: normalizeCode(value.code ?? ''),
      name: (value.name ?? '').trim(),
      displayOrder: Number(value.displayOrder ?? 0),
      defaultCartonNetKg: nullIfBlank(value.defaultCartonNetKg),
      defaultPricePerKg: price,
      defaultCurrency: currency,
    };
    this.gradeSubmitting.set(true);
    const request$ = current
      ? this.catalog.updateGrade(product.id, current.id, {
          expectedVersion: current.version,
          ...base,
          active: value.active ?? true,
        })
      : this.catalog.createGrade(product.id, { ...base, active: true });

    request$.subscribe({
      next: (grade) => {
        this.gradeSubmitting.set(false);
        this.notifications.success(
          current ? `Grade ${grade.code} updated.` : `Grade ${grade.code} created.`,
        );
        this.cancelGradeForm();
        this.loadGrades(this.grades().page);
      },
      error: (error: unknown) => {
        this.gradeSubmitting.set(false);
        this.handleError(error, this.gradeForm, 'The grade could not be saved.');
      },
    });
  }

  protected toggleGradeActive(grade: Grade): void {
    const product = this.selectedProduct();
    if (!product) {
      return;
    }
    this.catalog
      .updateGrade(product.id, grade.id, {
        expectedVersion: grade.version,
        active: !grade.active,
      })
      .subscribe({
        next: (updated) => {
          this.notifications.success(
            updated.active
              ? `Grade ${updated.code} activated.`
              : `Grade ${updated.code} deactivated.`,
          );
          this.loadGrades(this.grades().page);
        },
        error: (error: unknown) => this.handleError(error, null, 'The grade could not be updated.'),
      });
  }

  protected goToGradePage(page: number): void {
    if (page < 0 || page >= this.grades().totalPages || page === this.grades().page) {
      return;
    }
    this.loadGrades(page);
  }

  // ---- Shared helpers -------------------------------------------------------

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.page().totalPages || page === this.page().page) {
      return;
    }
    this.load(page);
  }

  protected showError(controlName: string, form: FormGroup): boolean {
    const control = form.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  protected showPairingError(): boolean {
    return (
      this.gradeForm.hasError('priceCurrencyPairing') &&
      (this.gradeForm.controls.defaultPricePerKg.dirty ||
        this.gradeForm.controls.defaultCurrency.dirty ||
        this.gradeForm.touched)
    );
  }

  protected formatKg(value: string | null): string {
    return value ? `${value} kg` : '—';
  }

  protected formatPrice(grade: Grade): string {
    return grade.defaultPricePerKg && grade.defaultCurrency
      ? `${grade.defaultPricePerKg} ${grade.defaultCurrency}`
      : '—';
  }

  private load(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.catalog
      .listProducts({
        page,
        size: PAGE_SIZE,
        q: this.searchTerm() || undefined,
        includeInactive: this.includeInactive(),
      })
      .subscribe({
        next: (result) => {
          this.page.set(result);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Unable to load products. Please try again.');
          this.loading.set(false);
        },
      });
  }

  private loadGrades(page: number): void {
    const product = this.selectedProduct();
    if (!product) {
      return;
    }
    this.gradesLoading.set(true);
    this.gradesError.set(null);
    this.catalog
      .listGrades(product.id, { page, size: PAGE_SIZE, includeInactive: this.includeInactive() })
      .subscribe({
        next: (result) => {
          this.grades.set(result);
          this.gradesLoading.set(false);
        },
        error: () => {
          this.gradesError.set('Unable to load grades. Please try again.');
          this.gradesLoading.set(false);
        },
      });
  }

  private handleError(error: unknown, form: FormGroup | null, fallback: string): void {
    if (isApiProblemError(error)) {
      if (form) {
        for (const fieldError of error.fieldErrors) {
          const control = form.get(fieldError.field);
          if (control) {
            control.setErrors({ server: fieldError.message });
            control.markAsTouched();
          }
        }
      }
      this.notifications.error(error.problem.detail || error.problem.title || fallback);
      return;
    }
    this.notifications.error(fallback);
  }
}

function priceCurrencyPairing(control: AbstractControl): ValidationErrors | null {
  const group = control as FormGroup;
  const price = (group.get('defaultPricePerKg')?.value ?? '').toString().trim();
  const currency = (group.get('defaultCurrency')?.value ?? '').toString().trim();
  if (price.length === 0 && currency.length === 0) {
    return null;
  }
  return price.length > 0 && currency.length > 0 ? null : { priceCurrencyPairing: true };
}
