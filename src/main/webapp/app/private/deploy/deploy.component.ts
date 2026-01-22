import {AfterViewInit, Component} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {lastValueFrom} from "rxjs";
import {MessageService} from "primeng/api";
import {ExternalService} from "../../shared/service/external.service";
import {StateStorageService} from "../../shared/service/state-storage.service";
import {NgIf} from "@angular/common";
import {PageComponent} from "../../shared/components/page/page.component";
import {MenuService} from "../layout/service/app-menu.service";

@Component({
  standalone: true,
  template: `
    <sm-page>
      <div class="card grid justify-content-center text-xl font-medium" *ngIf="showTemplate">
        Your app is initializing...
      </div>
    </sm-page>
  `,
  imports: [
    PageComponent,
    NgIf
  ]
})
export class DeployComponent implements AfterViewInit {
  showTemplate: boolean;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private externalService: ExternalService,
              private messageService: MessageService,
              private menuService: MenuService,
              private stateStorageService: StateStorageService) {}

  async ngAfterViewInit() {
    this.route.queryParams.subscribe(async params => {
      const referrer = this.stateStorageService.getReferrer();
      if (!referrer || !referrer.indexOf('github.com')) {
        await this.errorState('Bad referrer!');
        return;
      }

      this.stateStorageService.clearReferrer();
      this.stateStorageService.clearUrl();

      await this.deploy(params['appname'], params['subpath'], params['port'], referrer);
    });
  }

  private async deploy(appName: string, subPath: string, port: string, referrer: string) {
    try {
      Promise.resolve(null).then(() => this.showTemplate = true);
      const app = await lastValueFrom(this.externalService.deploy({appName, subPath, port, referrer}));
      await this.router.navigate(['/apps'], {queryParams: { id: app.body.id }});
      this.menuService.reload(app.body.project?.id);
    } catch(err) {
      this.showTemplate = false;
      await this.errorState('We could not process your request for the moment. Please try again later.');
    }
  }

  private async errorState(message: string) {
    Promise.resolve(null).then(() => {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: message
      });
    });
    setTimeout(() => this.router.navigate(['/']), 1000);
  }
}
