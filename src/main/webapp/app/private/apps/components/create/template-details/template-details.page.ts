import { Component } from '@angular/core';
import { PageComponent } from '../../../../../shared/components/page/page.component';
import { TemplateDetailsComponent } from '../../../../../shared/components/template-details/template-details.component';

@Component({
  templateUrl: './template-details.page.html',
  styleUrls: ['./template-details.page.scss'],
  standalone: true,
  imports: [
    PageComponent,
    TemplateDetailsComponent
  ]
})
export class TemplateDetailsPage {
}
