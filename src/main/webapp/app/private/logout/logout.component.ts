import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {LoginService} from '../../shared/service/login.service';
import {Store} from "@ngxs/store";
import {LogoutAction} from "../../shared/state/actions";
import {LayoutService} from "../../shared/service/theme/app-layout.service";
import {StateStorageService} from "../../shared/service/state-storage.service";

@Component({
  standalone: true,
  selector: 'sm-logout',
  template: ``
})
export class LogoutComponent implements OnInit {
  constructor(
    private loginService: LoginService,
    private router: Router,
    private store: Store,
    private layoutService: LayoutService,
    private stateStorage: StateStorageService
  ) {}

  async ngOnInit() {
    // Reset theme to default first
    this.stateStorage.storeDefaultTheme();
    this.layoutService.resetThemeLocal();

    // Perform logout actions
    this.store.dispatch(new LogoutAction());
    this.loginService.logout();
    await this.router.navigate(['/login']);
  }
}
