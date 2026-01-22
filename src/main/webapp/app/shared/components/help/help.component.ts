import { Component, Input, OnInit } from '@angular/core';
import { PanelModule } from 'primeng/panel';
import { NgForOf } from '@angular/common';
import { TagModule } from 'primeng/tag';
import { LayoutService } from '../../service/theme/app-layout.service';

@Component({
  standalone: true,
  selector: 'sm-help',
  imports: [
    PanelModule,
    NgForOf,
    TagModule
  ],
  template: `
    <div class="flex justify-content-start flex-wrap column-gap-4 my-3">
      <p-tag *ngFor="let item of items" severity="secondary" styleClass="cursor-pointer hover:bg-gray-200">
        <a class="text-decoration-none text-900 font-normal flex align-items-center gap-2 p-1 text-sm" [href]="item.link" target="_blank">
          <i class="pi pi-info-circle"></i>
          <span>{{item.title}}</span>
        </a>
      </p-tag>
    </div>
  `
})
export class HelpComponent {
  get items() {
    return this.layoutService.state.helpItems;
  }

  constructor(
    private layoutService: LayoutService,
  ) {
  }
}
