import { PROJECT_SERVICE } from '../../../core/services/project.service';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EMPTY, expand, forkJoin, map, reduce } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';
import {
  Product,
  Grade,
  CURRENCY_PATTERN,
  normalizeCurrency,
} from '../../../core/models/commercial.model';
import {
  CARTON_COUNT_MIN,
  PROJECT_LOT_REFERENCE_MAX_LENGTH,
  PROJECT_PRICE_PATTERN,
  PROJECT_QUANTITY_PATTERN,
  PROJECT_QUANTITY_UNIT_MAX_LENGTH,
  ProjectProduct,
  nullIfBlank,
} from '../../../core/models/master-data.model';
import { isApiProblemError } from '../../../core/models/project.model';
import { AuthStore } from '../../../core/services/auth.store';
import { CATALOG_SERVICE } from '../../../core/services/catalog.service';
import { NotificationService } from '../../../core/services/notification.service';
import { SHIPMENT_PRODUCT_SERVICE } from '../../../core/services/shipment-product.service';
import { trimmedRequired } from '../../../core/utils/validators';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';

/**
 * Shipment product lines. The master-data contract
 * stores these per project; every write carries the project version so parallel
 * edits are detected server-side.
 */
@Component({
  selector: 'app-shipment-products',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, IconComponent, PanelComponent],
  templateUrl: './shipment-products.component.html',
  styleUrl: './shipment-products.component.scss',
})
export class ShipmentProductsComponent implements OnInit {
  readonly projectId = input.required<string>();
  readonly projectVersion = input.required<number>();
  readonly changed = output<number>();
  readonly canEdit = input(false);

  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly projects = inject(PROJECT_SERVICE);
  private readonly masterData = inject(SHIPMENT_PRODUCT_SERVICE);
  private readonly catalog = inject(CATALOG_SERVICE);
  private readonly auth = inject(AuthStore);
  private readonly notifications = inject(NotificationService);

  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly canWrite = this.canEdit;

  protected readonly version = signal(0);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly savingProduct = signal(false);

  protected readonly projectProducts = signal<ProjectProduct[]>([]);
  protected readonly productOptions = signal<Product[]>([]);
  protected readonly categories = signal<Grade[]>([]);

  protected readonly productMode = signal<'add' | 'edit'>('add');
  protected readonly editingProduct = signal<ProjectProduct | null>(null);

  protected readonly cartonMin = CARTON_COUNT_MIN;
  protected readonly unitMax = PROJECT_QUANTITY_UNIT_MAX_LENGTH;
  protected readonly lotMax = PROJECT_LOT_REFERENCE_MAX_LENGTH;

  protected readonly productForm = this.fb.group({
    productId: ['', [trimmedRequired]],
    productCategoryId: [null as string | null],
    cartonCount: [null as number | null, [Validators.min(CARTON_COUNT_MIN)]],
    quantity: ['', [Validators.pattern(PROJECT_QUANTITY_PATTERN)]],
    quantityUnit: ['', [Validators.maxLength(PROJECT_QUANTITY_UNIT_MAX_LENGTH)]],
    netWeightKg: ['', [Validators.pattern(PROJECT_QUANTITY_PATTERN)]],
    grossWeightKg: ['', [Validators.pattern(PROJECT_QUANTITY_PATTERN)]],
    unitPrice: ['', [Validators.pattern(PROJECT_PRICE_PATTERN)]],
    currency: ['', [Validators.pattern(CURRENCY_PATTERN)]],
    lotReference: ['', [Validators.maxLength(PROJECT_LOT_REFERENCE_MAX_LENGTH)]],
    processingInformation: [''],
  });

  constructor() {
    effect(() => {
      const version = this.projectVersion();
      if (version > this.version()) {
        this.version.set(version);
      }
    });
  }

  ngOnInit(): void {
    this.productForm.controls.productId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((id) => {
        this.productForm.controls.productCategoryId.setValue(null);
        this.loadGrades(id ?? '');
      });
    this.load();
  }

  protected loadGrades(productId: string): void {
    this.categories.set([]);
    if (productId)
      this.catalog.listGrades(productId, { size: 100 }).subscribe({
        next: (page) => this.categories.set(page.items),
        error: () => this.notifications.error('Unable to load product grades.'),
      });
  }

  protected productName(productId: string): string {
    const option = this.productOptions().find((product) => product.id === productId);
    return option ? `${option.name} (${option.code})` : productId;
  }

  protected categoryName(categoryId: string | null | undefined): string {
    if (!categoryId) {
      return '—';
    }
    return this.categories().find((category) => category.id === categoryId)?.name ?? categoryId;
  }

  protected startAddProduct(): void {
    this.productForm.controls.productId.enable({ emitEvent: false });
    this.categories.set([]);
    this.productMode.set('add');
    this.editingProduct.set(null);
    this.productForm.reset({
      productId: '',
      productCategoryId: null,
      cartonCount: null,
      quantity: '',
      quantityUnit: '',
      netWeightKg: '',
      grossWeightKg: '',
      unitPrice: '',
      currency: '',
      lotReference: '',
      processingInformation: '',
    });
  }

  protected startEditProduct(product: ProjectProduct): void {
    this.productMode.set('edit');
    this.editingProduct.set(product);
    this.loadGrades(product.productId);
    this.productForm.reset({
      productId: product.productId,
      productCategoryId: product.productCategoryId,
      cartonCount: product.cartonCount,
      quantity: product.quantity ?? '',
      quantityUnit: product.quantityUnit ?? '',
      netWeightKg: product.netWeightKg ?? '',
      grossWeightKg: product.grossWeightKg ?? '',
      unitPrice: product.unitPrice ?? '',
      currency: product.currency ?? '',
      lotReference: product.lotReference ?? '',
      processingInformation: product.processingInformation ?? '',
    });
    this.productForm.controls.productId.disable({ emitEvent: false });
  }

  protected cancelProductForm(): void {
    this.editingProduct.set(null);
    this.productMode.set('add');
    this.productForm.controls.productId.enable({ emitEvent: false });
    this.productForm.reset({
      productId: '',
      productCategoryId: null,
      cartonCount: null,
      quantity: '',
      quantityUnit: '',
      netWeightKg: '',
      grossWeightKg: '',
      unitPrice: '',
      currency: '',
      lotReference: '',
      processingInformation: '',
    });
  }

  protected submitProduct(): void {
    if (this.productForm.invalid || this.savingProduct()) {
      this.productForm.markAllAsTouched();
      return;
    }
    const value = this.productForm.getRawValue();
    const price = this.isAdmin() ? nullIfBlank(value.unitPrice) : null;
    const currency = this.isAdmin() ? nullIfBlank(normalizeCurrency(value.currency ?? '')) : null;
    if (this.isAdmin() && ((price && !currency) || (!price && currency))) {
      this.notifications.error('Provide both price and currency.');
      return;
    }
    const measurement = {
      cartonCount: value.cartonCount != null ? Number(value.cartonCount) : null,
      quantity: nullIfBlank(value.quantity),
      quantityUnit: nullIfBlank(value.quantityUnit),
      netWeightKg: nullIfBlank(value.netWeightKg),
      grossWeightKg: nullIfBlank(value.grossWeightKg),
      lotReference: nullIfBlank(value.lotReference),
      processingInformation: nullIfBlank(value.processingInformation),
    };
    const editing = this.editingProduct();
    this.savingProduct.set(true);
    const request$ = editing
      ? this.masterData.updateProduct(this.projectId(), editing.id, {
          expectedVersion: editing.version,
          expectedProjectVersion: this.version(),
          productCategoryId: value.productCategoryId,
          ...measurement,
          ...(this.isAdmin() &&
          (this.productForm.controls.unitPrice.dirty || this.productForm.controls.currency.dirty)
            ? { unitPrice: price, currency }
            : {}),
        })
      : this.masterData.addProduct(this.projectId(), {
          expectedProjectVersion: this.version(),
          productId: value.productId ?? '',
          productCategoryId: value.productCategoryId,
          ...measurement,
          ...(this.isAdmin() &&
          (this.productForm.controls.unitPrice.dirty || this.productForm.controls.currency.dirty)
            ? { unitPrice: price, currency }
            : {}),
        });
    request$.subscribe({
      next: (product) => {
        this.savingProduct.set(false);
        this.version.set(product.projectVersion);
        this.notifications.success(
          editing ? 'Shipment product updated.' : 'Shipment product added.',
        );
        this.cancelProductForm();
        this.loadProducts();
        this.changed.emit(product.projectVersion);
      },
      error: (error: unknown) => {
        this.savingProduct.set(false);
        this.handleError(error, 'The shipment product could not be saved.');
      },
    });
  }

  protected showProductError(name: string): boolean {
    const control = this.productForm.get(name);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      project: this.projects.get(this.projectId()),
      products: this.masterData.listProducts(this.projectId()),
      catalogue: this.catalog.listProducts({ size: 100 }).pipe(
        expand((page) =>
          page.page + 1 < page.totalPages
            ? this.catalog.listProducts({ page: page.page + 1, size: 100 })
            : EMPTY,
        ),
        map((page) => page.items),
        reduce((all, items) => all.concat(items), [] as Product[]),
      ),
    }).subscribe({
      next: ({ project, products, catalogue }) => {
        this.version.set(project.version);
        this.projectProducts.set(products);
        this.productOptions.set(catalogue);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load shipment products.');
        this.loading.set(false);
      },
    });
  }

  private loadProducts(): void {
    this.masterData.listProducts(this.projectId()).subscribe({
      next: (items) => this.projectProducts.set(items),
      error: () => this.projectProducts.set([]),
    });
  }

  private handleError(error: unknown, fallback: string): void {
    if (isApiProblemError(error)) {
      if (error.status === 409) {
        this.notifications.error('This shipment changed. Reloading the latest version.');
        this.load();
        return;
      }
      for (const fieldError of error.fieldErrors) {
        const control = this.productForm.get(fieldError.field);
        if (control) {
          control.setErrors({ server: fieldError.message });
          control.markAsTouched();
        }
      }
      this.notifications.error(error.problem.detail || error.problem.title || fallback);
      return;
    }
    this.notifications.error(fallback);
  }
}
