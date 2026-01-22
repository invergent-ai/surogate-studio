import { Component, Input, OnChanges, SimpleChanges, ElementRef } from '@angular/core';
import { Notebook } from 'notebook-viewer-ts';

@Component({
  selector: 'sm-ipynb-renderer',
  standalone: true,
  template: `
    <div class="notebook-wrapper">
      <div class="notebook-container" #notebookContainer></div>
    </div>
  `,
  styles: [`
    .notebook-wrapper {
      background: #fafafa;
      border: 1px solid #ddd;
      border-top: none;
      border-radius: 0 0 6px 6px;
      box-shadow: 0 2px 5px rgba(0,0,0,0.1);
      padding: 15px;

      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    }

    .notebook-container {
      max-width: 900px;
      margin: auto;
    }

    .cell-code {
      background: #f7f7f7;
      border: 1px solid #ddd;
      border-radius: 4px;
      margin: 20px 0;
      padding: 10px;
    }

    .cell-markdown {
      margin: 15px 0;
      font-size: 15px;
      line-height: 1.6;
    }
  `]
})
export class IpynbRendererComponent implements OnChanges {
  @Input() text!: string;

  constructor(private el: ElementRef) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes['text'] && this.text) {
      this.renderNotebook();
    }
  }

  private renderNotebook() {
    try {
      const parsed = JSON.parse(this.text);
      const notebook = new Notebook(parsed);
      const html = notebook.render('none');

      const container = this.el.nativeElement.querySelector('.notebook-container');
      container.innerHTML = html;
    } catch (e) {
      console.error('❌ Invalid IPYNB JSON:', e);
    }
  }
}
