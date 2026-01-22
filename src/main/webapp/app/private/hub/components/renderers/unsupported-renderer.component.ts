import {Component, input} from '@angular/core';
import {MessageModule} from "primeng/message";

@Component({
  selector: "sm-unsupported-renderer",
  standalone: true,
  imports: [
    MessageModule
  ],
  template: `
    <p-message severity="warn" [text]="message()"></p-message>`
})
export class UnsupportedRendererComponent {
  message = input.required<string>();

}
