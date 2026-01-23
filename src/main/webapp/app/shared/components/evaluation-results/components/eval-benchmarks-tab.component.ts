// src/app/shared/components/evaluation-results/components/eval-benchmarks-tab.component.ts
import { Component, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { IEvaluationResult } from '../../../model/evaluation-result.model';
import { EvaluationResultsHelperService } from '../evaluation-results-helper.service';

@Component({
  standalone: true,
  selector: 'sm-eval-benchmarks-tab',
  imports: [CommonModule, TableModule, TagModule],
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
        }
      }
    }
  `,
})
export class EvalBenchmarksTabComponent {
  @Input() result: IEvaluationResult | null = null;

  helper = inject(EvaluationResultsHelperService);
}
