import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  FORM_KEYS,
  FORM_KEY_LABELS,
  FormKey,
  OperatorAssignment,
  User,
} from '../../../core/models/account.model';
import { Project, isApiProblemError } from '../../../core/models/project.model';
import { ACCOUNT_SERVICE } from '../../../core/services/account.service';
import { ASSIGNMENT_SERVICE } from '../../../core/services/assignment.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProjectStore } from '../../../core/services/project.store';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PanelComponent } from '../../../shared/components/panel/panel.component';

const USER_SCAN_SIZE = 100;

@Component({
  selector: 'app-project-access',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, IconComponent, PageHeaderComponent, PanelComponent],
  templateUrl: './project-access.component.html',
  styleUrl: './project-access.component.scss',
})
export class ProjectAccessComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(ProjectStore);
  private readonly accounts = inject(ACCOUNT_SERVICE);
  private readonly assignmentService = inject(ASSIGNMENT_SERVICE);
  private readonly notifications = inject(NotificationService);

  protected projectId = '';

  protected readonly formKeys = FORM_KEYS;
  protected readonly formKeyLabels = FORM_KEY_LABELS;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly project = signal<Project | null>(null);
  protected readonly assignments = signal<OperatorAssignment[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly editingUserId = signal<string | null>(null);

  protected readonly operators = computed(() =>
    this.users().filter((user) => user.active && user.roles.includes('OPERATOR')),
  );

  protected readonly form = this.fb.group({
    userId: ['', [Validators.required]],
    formKeys: this.formKeysGroup(),
  });

  ngOnInit(): void {
    this.projectId = this.route.snapshot.paramMap.get('id') ?? '';
    this.reload();
  }

  protected userLabel(userId: string): string {
    const user = this.users().find((item) => item.id === userId);
    return user ? `${user.displayName} (${user.username})` : userId;
  }

  protected formKeyLabel(key: FormKey): string {
    return this.formKeyLabels[key];
  }

  protected isChecked(key: FormKey): boolean {
    return this.form.controls.formKeys.get(key)?.value === true;
  }

  protected editAssignment(assignment: OperatorAssignment): void {
    this.editingUserId.set(assignment.userId);
    this.form.controls.userId.setValue(assignment.userId);
    this.form.controls.userId.disable();
    for (const key of FORM_KEYS) {
      this.form.controls.formKeys.get(key)?.setValue(assignment.formKeys.includes(key));
    }
  }

  protected cancelEdit(): void {
    this.editingUserId.set(null);
    this.form.controls.userId.enable();
    this.form.reset({ userId: '' });
    this.resetFormKeys();
  }

  protected save(): void {
    const userId = this.form.controls.userId.value;
    if (!userId || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.assignmentService.assign(this.projectId, userId, this.selectedFormKeys()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.notifications.success('Assignment saved.');
        this.cancelEdit();
        this.loadAssignments();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.notifications.error(
          isApiProblemError(error)
            ? (error.problem.detail ?? error.problem.title)
            : 'The assignment could not be saved.',
        );
      },
    });
  }

  protected remove(assignment: OperatorAssignment): void {
    this.assignmentService.unassign(this.projectId, assignment.userId).subscribe({
      next: () => {
        this.notifications.success('Operator unassigned.');
        if (this.editingUserId() === assignment.userId) {
          this.cancelEdit();
        }
        this.loadAssignments();
      },
      error: () => this.notifications.error('The operator could not be unassigned.'),
    });
  }

  private reload(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      project: this.store.getProject(this.projectId),
      assignments: this.assignmentService.listOperators(this.projectId),
      users: this.accounts.list(0, USER_SCAN_SIZE),
    }).subscribe({
      next: ({ project, assignments, users }) => {
        this.project.set(project);
        this.assignments.set(assignments);
        this.users.set(users.items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load project access. Please try again.');
        this.loading.set(false);
      },
    });
  }

  private loadAssignments(): void {
    this.assignmentService.listOperators(this.projectId).subscribe({
      next: (assignments) => this.assignments.set(assignments),
    });
  }

  private formKeysGroup() {
    return this.fb.group({
      PROJECT_SETUP: [false],
      INVOICE: [false],
      PACKING_LIST: [false],
      CATCH_CERTIFICATES: [false],
      PROCESSING: [false],
      TRANSPORT: [false],
      HEALTH_CERTIFICATE: [false],
      BILL_OF_LADING: [false],
      CERTIFICATE_OF_ORIGIN: [false],
      SHIPPING_LIST: [false],
    });
  }

  private resetFormKeys(): void {
    for (const key of FORM_KEYS) {
      this.form.controls.formKeys.get(key)?.setValue(false);
    }
  }

  private selectedFormKeys(): FormKey[] {
    return FORM_KEYS.filter((key) => this.form.controls.formKeys.get(key)?.value === true);
  }
}
