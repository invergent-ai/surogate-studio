// src/app/shared/components/evaluation-results/components/eval-benchmarks-tab.component.ts
import { Component, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { IEvaluationResult } from '../../../model/evaluation-result.model';
import { EvaluationResultsHelperService } from '../evaluation-results-helper.service';

@Component({
  standalone: true,
  selector: 'sm-eval-benchmarks-tab',
  imports: [CommonModule, TableModule, TagModule, ButtonModule],
  template: `
    @for (target of result?.targets; track target.name) {
      @if (target.benchmarks && target.benchmarks.length > 0) {
        <h4 class="text-sm font-semibold mb-2">{{ target.name }} ({{ target.model }})</h4>
        <p-table [value]="helper.getNonCustomBenchmarks(target.benchmarks)" styleClass="p-datatable-sm">
          <ng-template pTemplate="header">
            <tr>
              <th>Benchmark</th>
              <th>Backend</th>
              <th>Score</th>
              <th>Samples</th>
              <th>Status</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-benchmark>
            <tr>
              <td>{{ benchmark.benchmark_name }}</td>
              <td>{{ benchmark.backend }}</td>
              <td>
                <span class="font-semibold">{{ benchmark.overall_score | number: '1.2-4' }}</span>
              </td>
              <td>{{ benchmark.num_samples }}</td>
              <td>
                <p-tag [severity]="helper.getStatusSeverity(benchmark.status)" [value]="benchmark.status"></p-tag>
              </td>
            </tr>
          </ng-template>
        </p-table>

        @for (benchmark of helper.getNonCustomBenchmarks(target.benchmarks); track benchmark.benchmark_name) {
          <!-- Task Breakdown -->
          @if (benchmark.task_results && helper.getTaskResultEntries(benchmark.task_results).length > 0) {
            <div class="mt-3 mb-4">
              <h5 class="text-xs font-semibold mb-2 text-500">{{ benchmark.benchmark_name }} - Task Breakdown</h5>
              <p-table [value]="helper.getTaskResultEntries(benchmark.task_results)" styleClass="p-datatable-sm">
                <ng-template pTemplate="header">
                  <tr>
                    <th>Task</th>
                    <th>Score</th>
                    <th>Accuracy</th>
                    <th>Samples</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-task>
                  <tr>
                    <td>{{ task.name }}</td>
                    <td>{{ task.result.score | number: '1.2-4' }}</td>
                    <td>{{ task.result.accuracy | percent: '1.1-1' }}</td>
                    <td>{{ task.result.n_samples }}</td>
                  </tr>
                </ng-template>
              </p-table>
            </div>
          }

          <!-- Detailed Results -->
          @if (benchmark.detailed_results && benchmark.detailed_results.length > 0) {
            <div class="mt-3 mb-4">
              <h5 class="text-xs font-semibold mb-2 text-500">{{ benchmark.benchmark_name }} - Detailed Results</h5>
              <p-table
                [value]="benchmark.detailed_results"
                styleClass="p-datatable-sm"
                [paginator]="benchmark.detailed_results.length > 10"
                [rows]="10"
                [rowsPerPageOptions]="[10, 25, 50]"
                dataKey="metadata.index"
                [expandedRowKeys]="expandedRows"
              >
                <ng-template pTemplate="header">
                  <tr>
                    <th style="width: 3rem"></th>
                    <th>#</th>
                    <th>Subset</th>
                    <th>Score</th>
                    <th>Status</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-detail let-expanded="expanded" let-rowIndex="rowIndex">
                  <tr>
                    <td>
                      <p-button
                        type="button"
                        pRipple
                        [pRowToggler]="detail"
                        [text]="true"
                        [rounded]="true"
                        [plain]="true"
                        [icon]="expanded ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"
                      />
                    </td>
                    <td>{{ detail.metadata?.index ?? rowIndex }}</td>
                    <td>{{ detail.metadata?.metadata?.subject || detail.subset || '-' }}</td>
                    <td>{{ detail.score | number: '1.2-2' }}</td>
                    <td>
                      <p-tag [severity]="detail.success ? 'success' : 'danger'" [value]="detail.success ? 'Correct' : 'Incorrect'"></p-tag>
                    </td>
                  </tr>
                </ng-template>
                <ng-template pTemplate="rowexpansion" let-detail>
                  <tr>
                    <td colspan="5">
                      <div class="p-3 surface-50 border-round">
                        <!-- Model Output -->
                        @if (getModelOutput(detail)) {
                          <div class="mb-3">
                            <span class="font-semibold text-xs text-500">Model Output:</span>
                            <pre
                              class="mt-1 p-2 surface-100 border-round text-xs overflow-auto"
                              style="max-height: 300px; white-space: pre-wrap;"
                              >{{ getModelOutput(detail) }}</pre
                            >
                          </div>
                        }

                        <!-- Expected Answer -->
                        @if (detail.expected) {
                          <div class="mb-3">
                            <span class="font-semibold text-xs text-500">Expected:</span>
                            <span class="ml-2 text-sm">{{ detail.expected }}</span>
                          </div>
                        }

                        <!-- Usage Stats -->
                        @if (detail.metadata?.model_output?.usage) {
                          <div class="flex gap-4 text-xs text-500">
                            <span>Input tokens: {{ detail.metadata.model_output.usage.input_tokens }}</span>
                            <span>Output tokens: {{ detail.metadata.model_output.usage.output_tokens }}</span>
                            <span>Total: {{ detail.metadata.model_output.usage.total_tokens }}</span>
                          </div>
                        }

                        <!-- Stop Reason -->
                        @if (getStopReason(detail)) {
                          <div class="mt-2">
                            <p-tag
                              [severity]="getStopReason(detail) === 'max_tokens' ? 'warning' : 'info'"
                              [value]="'Stop: ' + getStopReason(detail)"
                              styleClass="text-xs"
                            ></p-tag>
                          </div>
                        }
                      </div>
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            </div>
          }
        }
      }
    }
  `,
})
export class EvalBenchmarksTabComponent {
  @Input() result: IEvaluationResult | null = null;

  helper = inject(EvaluationResultsHelperService);
  expandedRows: { [key: string]: boolean } = {};

  getModelOutput(detail: any): string {
    // Try to extract from nested structure
    const content = detail.metadata?.model_output?.choices?.[0]?.message?.content;

    if (content) {
      // Handle structured content (reasoning + text)
      if (Array.isArray(content)) {
        return content
          .map((item: any) => {
            if (item.type === 'reasoning') return `<think>\n${item.reasoning}\n</think>`;
            if (item.type === 'text') return item.text;
            return '';
          })
          .filter(Boolean)
          .join('\n\n');
      }
      return content;
    }

    // Fallback to raw_output or output
    return detail.raw_output || detail.output || '';
  }

  getStopReason(detail: any): string | null {
    return detail.metadata?.model_output?.choices?.[0]?.stop_reason || null;
  }
}
