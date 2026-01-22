import { Component, input } from '@angular/core';
import { HighlightAuto } from 'ngx-highlightjs';
import { HighlightLineNumbers } from 'ngx-highlightjs/line-numbers';

@Component({
  selector: 'sm-text-renderer',
  standalone: true,
  imports: [
    HighlightLineNumbers,
    HighlightAuto
  ],
  template: `
    <div class="w-full h-full">
      <pre><code [highlightAuto]="data()" lineNumbers></code></pre>
    </div>
  `
})
export class TextRendererComponent {
  data = input.required<string>();
}
