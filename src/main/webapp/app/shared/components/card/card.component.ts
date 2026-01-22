import { AfterContentInit, Component, ContentChildren, Input, QueryList, TemplateRef } from '@angular/core';
import { CardModule } from 'primeng/card';
import { LucideAngularModule } from 'lucide-angular';
import { NgIf, NgTemplateOutlet } from '@angular/common';
import { PrimeTemplate } from 'primeng/api';

@Component({
  selector: 'sm-card',
  standalone: true,
  imports: [
    CardModule,
    LucideAngularModule,
    NgTemplateOutlet,
    NgIf
  ],
  template: `
    <p-card>
      <div *ngIf="header" class="flex align-items-center justify-content-between gap-2">
        <div class="flex-grow-1 flex gap-2">
          <i-lucide *ngIf="icon" [img]="icon" class="w-1.5rem h-1.5rem {{iconClass}}"></i-lucide>
          <p class="text-base font-semibold {{textClass}}">{{header}}</p>
        </div>
        <div *ngIf="toolbarTemplate">
          <ng-container *ngTemplateOutlet="toolbarTemplate"></ng-container>
        </div>
      </div>
      <div [class.pt-4]="padContent">
        <ng-content></ng-content>
      </div>
    </p-card>
  `
})
export class CardComponent implements AfterContentInit {
  @Input()
  header: string;
  @Input()
  icon: any;
  @Input()
  iconClass: any;
  @Input()
  textClass: any;
  @Input()
  padContent = true;

  @ContentChildren(PrimeTemplate) templates: QueryList<PrimeTemplate> | undefined;

  public toolbarTemplate: TemplateRef<any> | undefined;

  ngAfterContentInit() {
    this.templates?.forEach((item) => {
      switch (item.getType()) {
        case 'toolbar':
          this.toolbarTemplate = item.template;
          break;
      }
    });
  }
}
