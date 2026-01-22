import {Component, inject, OnInit} from '@angular/core';
import {PageComponent} from "../../../../../shared/components/page/page.component";
import {PageLoadComponent} from "../../../../../shared/components/page-load/page-load.component";
import SharedModule from "../../../../../shared/shared.module";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {CardModule} from "primeng/card";
import {FloatLabelModule} from "primeng/floatlabel";
import {revalidateForm} from "../../../../../shared/util/form.util";
import {User} from "../../../../../shared/model/user.model";
import {Authority} from "../../../../../config/constant/authority.constants";
import {MultiSelectModule} from "primeng/multiselect";
import {finalize} from "rxjs/operators";
import {displaySuccess} from "../../../../../shared/util/success.util";
import {displayError} from "../../../../../shared/util/error.util";
import {UserService} from "../../../../../shared/service/user.service";
import {Store} from "@ngxs/store";

@Component({
  selector: 'sm-user-form',
  standalone: true,
  imports: [
    PageComponent,
    PageLoadComponent,
    SharedModule,
    ReactiveFormsModule,
    CardModule,
    FloatLabelModule,
    MultiSelectModule
  ],
  templateUrl: './user-form.component.html',
})
export class UserFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  router = inject(Router);
  userService = inject(UserService);
  doNotMatch = false;
  loading = false;
  updatedUser: User = null;
  isEditMode: boolean = false;

  authorityOptions = Object.keys(Authority).map(key => ({
    label: key.replace(/_/g, ' '),
    value: Authority[key as keyof typeof Authority]
  }));

  constructor(private store: Store,
              private route: ActivatedRoute,) {
  }

  ngOnInit() {
    const login = this.route.snapshot.paramMap.get('login');
    if (login) {
      this.userService.getUser(login).subscribe({
        next: (response) => {
          this.updatedUser = response;
          this.isEditMode = true;
          this.updateFormValuesFromEntity();
        }
      })
    }
  }

  registerForm = this.fb.group({
    login: ['', [Validators.required,
      Validators.email,
      Validators.minLength(1),
      Validators.maxLength(50),
      Validators.pattern('^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$')]],
    authorities: this.fb.control<string[]>([], {validators: [Validators.required]})
  });


  register() {
    revalidateForm(this.registerForm);
    if (!this.registerForm.valid) {
      return;
    }

    this.doNotMatch = false;

    this.loading = true;
    const formValue = this.registerForm.getRawValue();
    const user: User = this.isEditMode
      ? {...this.updatedUser, ...formValue}
      : new User(null, formValue.login, formValue.authorities);

    const request$ = this.isEditMode
      ? this.userService.updateUser(user) // PUT
      : this.userService.createUser(user); // POST

    request$
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: () => {
          displaySuccess(this.store, 'Please check your email to confirm your account.');
          setTimeout(() => {
            this.registerForm.reset();
            window.location.href = '/admin/users';
          }, 2000);
        },
        error: response => displayError(this.store, response)
      });

  }

  updateFormValuesFromEntity() {
    this.registerForm.controls.login.setValue(this.updatedUser.login);
    this.registerForm.controls.authorities.setValue(this.updatedUser.authorities);
  }

  goBack() {
    this.router.navigate(["admin/users"]);
  }
}
