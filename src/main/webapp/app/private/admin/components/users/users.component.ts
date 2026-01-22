import {Component, inject, signal} from '@angular/core';
import {PageComponent} from "../../../../shared/components/page/page.component";
import {PageLoadComponent} from "../../../../shared/components/page-load/page-load.component";
import SharedModule from "../../../../shared/shared.module";
import {UserService} from "../../../../shared/service/user.service";
import {User} from "../../../../shared/model/user.model";
import {Router} from "@angular/router";
import {PaginatorModule, PaginatorState} from "primeng/paginator";
import {displaySuccess} from "../../../../shared/util/success.util";
import {displayError} from "../../../../shared/util/error.util";
import {Store} from "@ngxs/store";
import {CardComponent} from "../../../../shared/components/card/card.component";
import {Plus} from "lucide-angular";

@Component({
  selector: 'sm-users',
  standalone: true,
  imports: [
    PageComponent,
    PageLoadComponent,
    SharedModule,
    PaginatorModule,
    CardComponent
  ],
  templateUrl: './users.component.html',
})
export class UsersComponent {
  first: number = 0;
  rows: number = 5;
  store = inject(Store);

  users = signal<User[]>([]);

  userService = inject(UserService);
  router = inject(Router);
  totalRecords = 0;

  constructor() {
    this.loadUsers();
  }

  loadUsers() {
    const page = this.first / this.rows;
    const size = this.rows;

    this.userService.getAllUsers(page, size).subscribe({
      next: (response) => {
        this.users.set(response.body ?? []);
        const total = response.headers.get('X-Total-Count');
        this.totalRecords = total ? +total : 0;
      },
      error:(err)=>{
        console.log("eroare la preluarea userilor"+ err);
      }
    })
  }

  onPageChange(event: PaginatorState) {
    this.first = event.first;
    this.rows = event.rows;
    this.loadUsers();
  }

  onAddUser() {
    this.router.navigate(["admin/users/add"]);
  }

  onEditUser(user: User) {
    this.router.navigate(["admin/users/edit", user.login]);
  }

  onDeleteUser(user: User) {
    if (!confirm('Are you sure you want to delete this user?')) {
      return;
    }

    this.userService.deleteUser(user.login).subscribe({
      next: () => {
        displaySuccess(this.store, 'User deleted successfully!');
        this.loadUsers();
      },
      error: (response) => displayError(this.store, response),
    });
  }


  protected readonly Plus = Plus;
}
