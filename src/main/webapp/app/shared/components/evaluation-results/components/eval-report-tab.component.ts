// src/app/shared/components/evaluation-results/components/eval-report-tab.component.ts
import { Component, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonDirective } from 'primeng/button';
import { MarkdownModule } from 'ngx-markdown';
import { Download, LucideAngularModule } from 'lucide-angular';
import { EvaluationResultService } from '../../../service/evaluation-result.service';

@Component({
  standalone: true,
  selector: 'sm-eval-report-tab',
  imports: [CommonModule, ButtonDirective, MarkdownModule, LucideAngularModule],
  template: `
    <div class="flex justify-content-end mb-3">
      <button pButton size="small" severity="secondary" (click)="downloadPdf()" class="flex gap-2">
        <i-lucide [img]="Download" class="w-1rem h-1rem"></i-lucide>
        Download PDF
      </button>
    </div>
    @if (report) {
      <div class="markdown-content border-1 border-200 border-round p-3 overflow-auto" style="max-height: 400px;">
        <markdown [data]="report"></markdown>
      </div>
    } @else {
      <div class="text-center text-500 p-4">No report available</div>
    }
  `,
  styles: [
    `
      .markdown-content ::ng-deep {
        h1,
        h2,
        h3 {
          margin-top: 1rem;
          margin-bottom: 0.5rem;
          font-weight: 600;
        }
        p {
          margin-bottom: 0.75rem;
        }
        ul,
        ol {
          margin-left: 1.5rem;
          margin-bottom: 0.75rem;
        }
        code {
          background: var(--surface-200);
          padding: 0.125rem 0.375rem;
          border-radius: 4px;
        }
        table {
          width: 100%;
          border-collapse: collapse;
          margin: 1rem 0;
        }
        th,
        td {
          border: 1px solid var(--surface-300);
          padding: 0.5rem 0.75rem;
          text-align: left;
        }
        th {
          background: var(--surface-100);
          font-weight: 600;
        }
        tr:nth-child(even) {
          background: var(--surface-50);
        }
      }
    `,
  ],
})
export class EvalReportTabComponent {
  @Input() taskRunId?: string;
  @Input() report: string | null = null;

  private resultService = inject(EvaluationResultService);
  protected readonly Download = Download;

  downloadPdf(): void {
    if (this.taskRunId) this.resultService.downloadPdf(this.taskRunId);
  }
}
