import { AfterViewInit, Component, ElementRef, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import SharedModule from 'app/shared/shared.module';
import { finalize } from 'rxjs/operators';
import { LayoutService } from '../../shared/service/theme/app-layout.service';
import { RegisterService } from '../../shared/service/register.service';
import { revalidateForm } from '../../shared/util/form.util';
import { displayError } from '../../shared/util/error.util';
import { Store } from '@ngxs/store';
import { displaySuccess } from '../../shared/util/success.util';
import { Registration } from '../../shared/model/register.model';

@Component({
  selector: 'sm-register',
  standalone: true,
  imports: [SharedModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']

})
export default class RegisterComponent implements AfterViewInit {
  @ViewChild('login', {static: false})
  login?: ElementRef;

  doNotMatch = false;
  loading = false;
  referralCode: string;

  registerForm = new FormGroup({
    login: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.email,
        Validators.minLength(1),
        Validators.maxLength(50),
        Validators.pattern('^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$'),
      ],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(4), Validators.maxLength(50)],
    }),
    confirmed: new FormControl(false, {nonNullable: true, validators: []})
  });

  constructor(
    public layoutService: LayoutService,
    private store: Store,
    private route: ActivatedRoute,
    private translateService: TranslateService,
    private registerService: RegisterService,
  ) {
    this.referralCode = this.route.snapshot.queryParamMap.get('ref');
    console.log(this.referralCode);
    this.registerForm.get('confirmPassword')?.valueChanges.subscribe(() => {
      const password = this.registerForm.get('password')?.value;
      const confirmPassword = this.registerForm.get('confirmPassword')?.value;

      if (password && confirmPassword) {
        this.doNotMatch = password !== confirmPassword;
      } else {
        this.doNotMatch = false;
      }
    });
    this.registerForm.get('password')?.valueChanges.subscribe(() => {
      const password = this.registerForm.get('password')?.value;
      const confirmPassword = this.registerForm.get('confirmPassword')?.value;

      if (password && confirmPassword) {
        this.doNotMatch = password !== confirmPassword;
      }
    });
  }

  ngAfterViewInit(): void {
    if (this.login) {
      this.login.nativeElement.focus();
    }
  }

  register(): void {
    revalidateForm(this.registerForm);
    if (!this.registerForm.valid) {
      return;
    }

    this.doNotMatch = false;
    const {password, confirmPassword} = this.registerForm.getRawValue();
    if (password !== confirmPassword) {
      this.doNotMatch = true;
    } else {
      this.loading = true;
      const {login} = this.registerForm.getRawValue();
      this.registerService
        .save({
          login,
          email: login, password,
          langKey: this.translateService.currentLang,
          referralCode: this.referralCode
        } as Registration)
        .pipe(finalize(() => this.loading = false))
        .subscribe({
          next: () => {
            displaySuccess(this.store, 'Please check your email to confirm your account.');
            setTimeout(() => {
              this.registerForm.reset();
              window.location.href = '/login';
            }, 7000);
          },
          error: response => displayError(this.store, response)
        });
    }
  }
}
