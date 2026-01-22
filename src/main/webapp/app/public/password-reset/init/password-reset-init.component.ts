import { Component, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators, FormControl } from '@angular/forms';
import SharedModule from 'app/shared/shared.module';
import { RouterLink } from "@angular/router";
import { PasswordResetInitService } from '../../../shared/service/password-reset-init.service';
import { LayoutService } from '../../../shared/service/theme/app-layout.service';

@Component({
  selector: 'sm-password-reset-init',
  standalone: true,
  imports: [SharedModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './password-reset-init.component.html',
  styles: [`
    :host ::ng-deep {
      .card {
        background: var(--surface-card);
        backdrop-filter: blur(10px);
        border: 1px solid var(--surface-border);
      }

      .p-inputtext {
        background: var(--surface-ground);
        border: 1px solid var(--surface-border);
        transition: all 0.2s;

        &:hover {
          border-color: var(--primary-color);
        }

        &:focus {
          box-shadow: 0 0 0 2px var(--surface-ground),
          0 0 0 4px var(--primary-color);
        }
      }

      .p-button {
        transition: all 0.2s;

        &:not(:disabled):hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 8px rgba(0,0,0,0.1);
        }
      }

      .p-input-icon-left > i {
        left: 0.75rem;
        color: var(--text-color-secondary);
      }

      .p-input-icon-left > input {
        padding-left: 2.5rem;
      }
    }
  `]
})
export default class PasswordResetInitComponent implements AfterViewInit {
  @ViewChild('emailInput') emailInput?: ElementRef;

  success = false;
  loading = false;

  resetRequestForm = this.fb.group({
    email: new FormControl('', {
      validators: [
        Validators.required,
        Validators.minLength(5),
        Validators.maxLength(254),
        Validators.email
      ],
      nonNullable: true
    })
  });

  constructor(
    private passwordResetInitService: PasswordResetInitService,
    private fb: FormBuilder,
    public layoutService: LayoutService
  ) {}

  ngAfterViewInit(): void {
    if (this.emailInput) {
      this.emailInput.nativeElement.focus();
    }
  }

  requestReset(): void {
    if (this.resetRequestForm.valid) {
      this.loading = true;
      const emailControl = this.resetRequestForm.get('email');
      if (emailControl) {
        this.passwordResetInitService.save(emailControl.value)
          .subscribe({
            next: () => {
              this.success = true;
              this.loading = false;
            },
            error: () => {
              this.loading = false;
            }
          });
      }
    }
  }

  get dark(): boolean {
    return this.layoutService.config.colorScheme !== 'light';
  }
}
