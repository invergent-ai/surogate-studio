import {Component} from '@angular/core';
import {ToastModule} from "primeng/toast";

@Component({
  standalone: true,
  selector: "sm-page",
  template: `
    <div class="px-1 py-4">
      <p-toast></p-toast>
      <ng-content></ng-content>
    </div>
  `,
  imports: [
    ToastModule
  ],
})
export class PageComponent {

}
