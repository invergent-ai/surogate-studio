import { Component, ContentChild, Input, TemplateRef } from '@angular/core';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { NgIf, NgTemplateOutlet } from '@angular/common';

@Component({
  selector: 'sm-page-loader',
  standalone: true,
  imports: [
    ProgressSpinnerModule,
    NgIf,
    NgTemplateOutlet
  ],
  template: `
    <ng-container *ngIf="loading; else loaded">
      <div class="text-center mt-5 mb-5">
        <p-progressSpinner styleClass="w-2rem h-2rem" strokeWidth="8" animationDuration=".7s"></p-progressSpinner>
      </div>
      <div class="text-center mt-2 mb-5" *ngIf="message">{{ message }}</div>
    </ng-container>
    <ng-template #loaded>
      <ng-container *ngTemplateOutlet="bodyTemplate"></ng-container>
    </ng-template>
  `
})
export class PageLoadComponent {
  @Input()
  loading: boolean;
  @Input()
  message: string;

  @ContentChild(TemplateRef, { static: false })
  bodyTemplate: TemplateRef<any>;
}
