import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { lastValueFrom } from 'rxjs';
import { Store } from '@ngxs/store';
import { MarkdownComponent } from 'ngx-markdown';
import { NgIf } from '@angular/common';
import { PageLoadComponent } from '../page-load/page-load.component';
import { IAppTemplate } from '../../model/app-template.model';
import { AppTemplateService } from '../../service/app-template.service';
import { displayError } from '../../util/error.util';

@Component({
  standalone: true,
  selector: 'sm-template-details',
  templateUrl: './template-details.component.html',
  styleUrls: ['./template-details.component.scss'],
  imports: [PageLoadComponent, MarkdownComponent, NgIf],
})
export class TemplateDetailsComponent implements OnInit {
  @Input() public: boolean;

  template: IAppTemplate;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private appTemplateService: AppTemplateService,
    private store: Store,
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

  public goBack() {
    if (this.public) {
      this.router.navigate(['/', 'templates']);
    } else {
      this.router.navigate(['/', 'apps', 'deploy']);
    }
  }
}
