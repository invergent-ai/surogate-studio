// src/app/shared/components/evaluation-results/components/eval-custom-tab.component.ts
import { Component, inject, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AccordionModule } from 'primeng/accordion';
import { TooltipModule } from 'primeng/tooltip';
import { CheckCircle, LucideAngularModule, MessageSquare, XCircle } from 'lucide-angular';
import { IEvaluationResult } from '../../../model/evaluation-result.model';
import { EvaluationResultsHelperService } from '../evaluation-results-helper.service';
import { ICustomBenchmark, ICustomDetailedResult } from '../../../model/evaluation-results.models';

@Component({
  standalone: true,
  selector: 'sm-eval-custom-tab',
  imports: [CommonModule, TableModule, TagModule, AccordionModule, TooltipModule, LucideAngularModule],
  template: `
    @for (benchmark of helper.getCustomBenchmarks(result); track benchmark.benchmark_name) {
      <div class="mb-4">
        <div class="flex align-items-center justify-content-between mb-3">
          <div>
            <h4 class="text-sm font-semibold mb-1">{{ benchmark.benchmark_name }}</h4>
            <span class="text-xs text-500">{{ benchmark.backend }} · {{ benchmark.detailed_results?.length || 0 }} test cases</span>
          </div>
          <div class="flex align-items-center gap-2">
            <span class="text-lg font-bold" [class]="helper.getScoreClass(benchmark.overall_score)">
              {{ benchmark.overall_score | percent: '1.1-1' }}
            </span>
            <p-tag [severity]="helper.getStatusSeverity(benchmark.status)" [value]="benchmark.status"></p-tag>
          </div>
        </div>

        <!-- Task Results Summary -->
        @if (benchmark.task_results) {
          <div class="grid mb-3">
            @for (task of helper.getCustomTaskEntries(benchmark.task_results); track task.key) {
              <div class="col-6 md:col-4 lg:col-3">
                <div class="border-1 border-200 border-round p-3 text-center">
                  <div class="text-xs text-500 mb-1">{{ task.key | titlecase }}</div>
                  @if (task.value.accuracy !== undefined) {
                    <div class="text-xl font-semibold" [class]="helper.getScoreClass(task.value.accuracy)">
                      {{ task.value.accuracy | percent: '1.0-0' }}
                    </div>
                    <div class="text-xs text-400">{{ task.value.correct }}/{{ task.value.total }}</div>
                  } @else if (task.value.avg_score !== undefined) {
                    <div class="text-xl font-semibold" [class]="helper.getScoreClass(task.value.avg_score)">
                      {{ task.value.avg_score | number: '1.2-2' }}
                    </div>
                    <div class="text-xs text-400">{{ task.value.success_rate | percent: '1.0-0' }} success</div>
                  }
                </div>
              </div>
            }
          </div>
        }

        <!-- Detailed Results -->
        @if (benchmark.detailed_results && benchmark.detailed_results.length > 0) {
          <p-accordion>
            <p-accordionTab header="Detailed Results ({{ benchmark.detailed_results.length }})">
              <!-- Filter by eval_type -->
              <div class="flex gap-2 mb-3">
                <p-tag
                  [severity]="selectedEvalType() === 'all' ? 'info' : 'secondary'"
                  value="All"
                  [rounded]="true"
                  class="cursor-pointer"
                  (click)="selectedEvalType.set('all')"
                ></p-tag>
                @for (evalType of getUniqueEvalTypes(benchmark); track evalType) {
                  <p-tag
                    [severity]="selectedEvalType() === evalType ? 'info' : 'secondary'"
                    [value]="evalType | titlecase"
                    [rounded]="true"
                    class="cursor-pointer"
                    (click)="selectedEvalType.set(evalType)"
                  ></p-tag>
                }
              </div>

              <p-table
                [value]="filterDetailedResults(benchmark.detailed_results)"
                styleClass="p-datatable-sm"
                [paginator]="true"
                [rows]="10"
                [rowsPerPageOptions]="[5, 10, 25]"
              >
                <ng-template pTemplate="header">
                  <tr>
                    <th style="width: 50px">#</th>
                    <th>Instruction</th>
                    <th>Expected</th>
                    <th>Output</th>
                    <th style="width: 80px">Score</th>
                    <th style="width: 100px">Type</th>
                    <th style="width: 80px">Status</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-detail>
                  <tr>
                    <td class="text-xs text-500">{{ detail.original_idx }}</td>
                    <td>
                      <div
                        class="text-sm"
                        style="max-width: 250px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"
                        [pTooltip]="detail.instruction"
                        tooltipPosition="top"
                      >
                        {{ detail.instruction }}
                      </div>
                    </td>
                    <td>
                      <code class="text-xs bg-surface-100 px-2 py-1 border-round">{{ detail.expected }}</code>
                    </td>
                    <td>
                      <div class="flex align-items-center gap-2">
                        <code class="text-xs bg-surface-100 px-2 py-1 border-round">{{ detail.output || '(empty)' }}</code>
                        @if (detail.raw_output && detail.raw_output !== detail.output) {
                          <i-lucide
                            [img]="MessageSquare"
                            class="w-1rem h-1rem text-400 cursor-pointer"
                            [pTooltip]="'Raw: ' + detail.raw_output"
                            tooltipPosition="top"
                          ></i-lucide>
                        }
                      </div>
                    </td>
                    <td>
                      <span class="font-semibold" [class]="helper.getScoreClass(detail.score)">
                        {{ detail.score | number: '1.1-1' }}
                      </span>
                    </td>
                    <td>
                      <div class="flex flex-column gap-1">
                        <p-tag
                          [severity]="getEvalTypeSeverity(detail.eval_type)"
                          [value]="detail.eval_type"
                          [rounded]="true"
                          styleClass="text-xs"
                        ></p-tag>
                        @if (detail.format) {
                          <p-tag severity="secondary" [value]="detail.format" [rounded]="true" styleClass="text-xs"></p-tag>
                        }
                      </div>
                    </td>
                    <td>
                      <div class="flex align-items-center gap-2">
                        @if (detail.success) {
                          <i-lucide [img]="CheckCircle" class="w-1.25rem h-1.25rem text-green-500"></i-lucide>
                        } @else {
                          <i-lucide [img]="XCircle" class="w-1.25rem h-1.25rem text-red-500"></i-lucide>
                        }
                        @if (detail.reason) {
                          <i-lucide
                            [img]="MessageSquare"
                            class="w-1rem h-1rem text-400 cursor-pointer"
                            [pTooltip]="detail.reason"
                            tooltipPosition="left"
                          ></i-lucide>
                        }
                      </div>
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            </p-accordionTab>
          </p-accordion>
        }
      </div>
    }
  `,
})
export class EvalCustomTabComponent {
  @Input() result: IEvaluationResult | null = null;

  helper = inject(EvaluationResultsHelperService);
  selectedEvalType = signal<string>('all');

  protected readonly CheckCircle = CheckCircle;
  protected readonly XCircle = XCircle;
  protected readonly MessageSquare = MessageSquare;

  getUniqueEvalTypes(benchmark: ICustomBenchmark): string[] {
    if (!benchmark.detailed_results) return [];
    return [...new Set(benchmark.detailed_results.map(d => d.eval_type))];
  }

  filterDetailedResults(results: ICustomDetailedResult[]): ICustomDetailedResult[] {
    if (this.selectedEvalType() === 'all') return results;
    return results.filter(r => r.eval_type === this.selectedEvalType());
  }

  getEvalTypeSeverity(evalType: string): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    switch (evalType) {
      case 'exact_match':
        return 'info';
      case 'judge':
        return 'warning';
      default:
        return 'secondary';
    }
  }
}
