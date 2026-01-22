import {Component, input} from '@angular/core';
import {MessageModule} from "primeng/message";

@Component({
  selector: "sm-toolarge-renderer",
  standalone: true,
  imports: [
    MessageModule
  ],
  template: `
    <p-message severity="warn" [text]="message()"></p-message>`
})
export class ToolargeRendererComponent {
  message = input.required<string>();
}
