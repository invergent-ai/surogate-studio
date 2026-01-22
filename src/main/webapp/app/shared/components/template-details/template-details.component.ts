import {Component, Input, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {lastValueFrom} from 'rxjs';
import {Store} from '@ngxs/store';
import {MarkdownComponent} from 'ngx-markdown';
import {NgIf} from '@angular/common';
import {ButtonDirective} from 'primeng/button';
import {PageLoadComponent} from "../page-load/page-load.component";
import {IAppTemplate} from "../../model/app-template.model";
import {AppTemplateService} from "../../service/app-template.service";
import {displayError} from "../../util/error.util";
import {ApplicationService} from "../../service/application.service";
import {UserService} from "../../service/user.service";
import {AccountService} from "../../service/account.service";
import {IApplication} from "../../model/application.model";
import {IContainer} from "../../model/container.model";
import {dummyText} from "../../util/form.util";
import {MenuService} from "../../../private/layout/service/app-menu.service";
import {ApplicationMode} from "../../model/enum/application-mode.model";

@Component({
  standalone: true,
  selector: 'sm-template-details',
  templateUrl: './template-details.component.html',
  styleUrls: ['./template-details.component.scss'],
  imports: [
    PageLoadComponent,
    MarkdownComponent,
    NgIf,
    ButtonDirective
  ]
})
export class TemplateDetailsComponent implements OnInit {
  @Input() public: boolean;

  template: IAppTemplate;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private appTemplateService: AppTemplateService,
    private applicationService: ApplicationService,
    private userService: UserService,
    private accountService: AccountService,
    private store: Store,
    private menuService: MenuService
  ) {}

  async ngOnInit() {
    this.route.params.subscribe(async params => {
      const templateId = params['id'];
      if (templateId) {
        try {
          const response = await lastValueFrom(this.appTemplateService.find(templateId));
          if (response.body) {
            this.template = response.body;
          } else {
            displayError(this.store, 'Template not found');
          }
        } catch (e) {
          displayError(this.store, e);
        } finally {
          this.loading = false;
        }
      }
    });
  }

  async createAppFromTemplate() {
    if (this.public) {
      await this.router.navigate(['/login']);
      return;
    }

    const user = await lastValueFrom(this.accountService.identity(true));
    const app = JSON.parse(this.template.template) as IApplication;
    app.project = user.defaultProject;
    app.containers.forEach((container: IContainer) => {
      if (container.ports) {
        container.ports.forEach(port => {
          if (!port.name.endsWith("-x")) {
            port.name = (dummyText(5).toLowerCase() + port.name).substring(0, 7);
          }
        });
      }
      if (container.volumeMounts) {
        container.volumeMounts.forEach(volumeMount => {
          if (volumeMount.volume) {
            volumeMount.volume.name += dummyText(5).toLowerCase();
            volumeMount.volume.project = user.defaultProject;
          }
        });
      }
    });
    app.fromTemplate = true;
    if (!app.mode) {
      app.mode = ApplicationMode.APPLICATION; // For existing app templates with no mode set
    }

    const created = await lastValueFrom(this.applicationService.save(app));
    await this.router.navigate(['/apps'], {queryParams: { id: created.id }});
    this.menuService.reload(app.project?.id);
  }

  public goBack() {
    if (this.public) {
      this.router.navigate(['/','templates']);
    } else {
      this.router.navigate(['/','apps','deploy']);
    }
  }
}
