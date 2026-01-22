import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { FormGroup, FormControl, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import SharedModule from 'app/shared/shared.module';
import { PasswordResetFinishService } from '../../../shared/service/password-reset-finish.service';
import { LayoutService } from '../../../shared/service/theme/app-layout.service';

@Component({
  selector: 'sm-password-reset-finish',
  standalone: true,
  imports: [SharedModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './password-reset-finish.component.html',
  styleUrls: ['./password-reset-finish.component.scss']

})
export default class PasswordResetFinishComponent implements OnInit, AfterViewInit {
  @ViewChild('newPassword', { static: false })
  newPassword?: ElementRef;

  initialized = false;
  doNotMatch = false;
  error = false;
  success = false;
  key = '';
  loading = false;
  passwordsMatch = false;

  passwordForm = new FormGroup({
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
    }),
  });

  constructor(
    private passwordResetFinishService: PasswordResetFinishService,
    private route: ActivatedRoute,
    public layoutService: LayoutService
  ) {
    this.passwordForm.valueChanges.subscribe(() => {
      const { newPassword, confirmPassword } = this.passwordForm.getRawValue();
      this.passwordsMatch = newPassword === confirmPassword && newPassword !== '';
      this.doNotMatch = newPassword !== confirmPassword && confirmPassword !== '';
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['key']) {
        this.key = params['key'];
      }
      this.initialized = true;
    });
  }

  ngAfterViewInit(): void {
    if (this.newPassword) {
      this.newPassword.nativeElement.focus();
    }
  }

  finishReset(): void {
    this.doNotMatch = false;
    this.error = false;
    this.loading = true;

    const { newPassword, confirmPassword } = this.passwordForm.getRawValue();

    if (newPassword !== confirmPassword) {
      this.doNotMatch = true;
      this.loading = false;
    } else {
      this.passwordResetFinishService.save(this.key, newPassword).subscribe({
        next: () => {
          this.success = true;
          this.loading = false;
        },
        error: () => {
          this.error = true;
          this.loading = false;
        },
      });
    }
  }

  get dark(): boolean {
    return this.layoutService.config.colorScheme !== 'light';
  }
}
