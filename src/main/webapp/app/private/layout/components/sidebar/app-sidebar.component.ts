import { Component, ElementRef, ViewChild } from '@angular/core';
import { LayoutService } from '../../../../shared/service/theme/app-layout.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'sm-sidebar',
  templateUrl: './app-sidebar.component.html'
})
export class AppSidebarComponent {
  timeout: any = null;

  @ViewChild('menuContainer') menuContainer!: ElementRef;

  constructor(public layoutService: LayoutService, public _: ElementRef) {
  }


  protected readonly environment = environment;
}
