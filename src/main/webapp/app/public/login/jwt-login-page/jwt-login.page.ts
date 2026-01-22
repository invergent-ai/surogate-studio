import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {LoginService} from '../../../shared/service/login.service';
import {finalize} from 'rxjs/operators';
import {LayoutService} from '../../../shared/service/theme/app-layout.service';

@Component({
  standalone: true,
  selector: 'sm-jwt-login',
  template: ``,
})
export class JwtLoginPage implements OnInit {
  loading = true;

  constructor(
    public layoutService: LayoutService,
    private route: ActivatedRoute,
    private router: Router,
    private loginService: LoginService,
  ) {
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const jwt = params['token'];
      if (jwt) {
        this.loginService.loginWithToken(jwt)
          .pipe(finalize(() => this.loading = false))
          .subscribe({
            next: () => {
              if (!this.router.getCurrentNavigation()) {
                // There were no routing during login (eg from navigationToStoredUrl)
                this.router.navigate(['']);
              }
            },
            error: (_) => this.router.navigate(['/accessdenied']),
          });
      } else {
        this.router.navigate(['/accessdenied'])
      }
    });
  }
}
