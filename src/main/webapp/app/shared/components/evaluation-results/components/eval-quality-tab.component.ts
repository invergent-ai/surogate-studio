import { Component, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AccordionModule } from 'primeng/accordion';
import { TooltipModule } from 'primeng/tooltip';
import { ProgressBarModule } from 'primeng/progressbar';
import { DividerModule } from 'primeng/divider';
import { CheckCircle, LucideAngularModule, MessageSquare, XCircle } from 'lucide-angular';
import { IEvaluationResult, IEvaluation } from '../../../model/evaluation-result.model';
import { EvaluationResultsHelperService } from '../evaluation-results-helper.service';

interface ConversationTurn {
  role: 'user' | 'assistant';
  content: string;
}

@Component({
  standalone: true,
  selector: 'sm-eval-quality-tab',
  imports: [CommonModule, TableModule, TagModule, AccordionModule, TooltipModule, ProgressBarModule, DividerModule, LucideAngularModule],
  styles: [
    `
      ::ng-deep .success-high .p-progressbar-value {
        background: var(--green-500);
      }
      ::ng-deep .success-medium .p-progressbar-value {
        background: var(--yellow-500);
      }
      ::ng-deep .success-low .p-progressbar-value {
        background: var(--red-500);
      }
    `,
  ],
  template: `
    @for (target of result?.targets; track target.name) {
      @for (evaluation of filterEvaluations(target.evaluations); track evaluation.name) {
        @if (evaluation.metrics_summary) {
          <div class="mb-4">
            <div class="flex align-items-center justify-content-between mb-3">
              <div>
                <h4 class="text-sm font-semibold mb-1">{{ evaluation.name }}</h4>
                <span class="text-xs text-500">{{ evaluation.dataset }} · {{ evaluation.num_test_cases }} test cases</span>
              </div>
              <p-tag [severity]="helper.getStatusSeverity(evaluation.status)" [value]="evaluation.status || 'unknown'"></p-tag>
            </div>

            <p-table [value]="helper.getMetricsSummaryEntries(evaluation.metrics_summary)" styleClass="p-datatable-sm mb-3">
              <ng-template pTemplate="header">
                <tr>
                  <th>Metric</th>
                  <th>Type</th>
                  <th>Avg Score</th>
                  <th>Success Rate</th>
                  <th>Evaluations</th>
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-metric>
                <tr>
                  <td class="font-medium">{{ metric.key }}</td>
                  <td><p-tag severity="secondary" [value]="metric.value.metric_type" [rounded]="true"></p-tag></td>
                  <td>
                    <span class="font-semibold" [class]="helper.getScoreClass(metric.value.avg_score)">
                      {{ metric.value.avg_score | number: '1.2-2' }}
                    </span>
                  </td>
                  <td>
                    <div class="flex align-items-center gap-2">
                      <p-progressBar
                        [value]="metric.value.success_rate * 100"
                        [showValue]="false"
                        [style]="{ width: '60px', height: '6px' }"
                        [styleClass]="getSuccessRateBarClass(metric.value.success_rate)"
                      ></p-progressBar>
                      <span class="text-xs">{{ metric.value.success_rate | percent: '1.0-0' }}</span>
                    </div>
                  </td>
                  <td>{{ metric.value.num_evaluations }}</td>
                </tr>
              </ng-template>
            </p-table>

            @if (evaluation.detailed_results?.length) {
              <p-accordion>
                <p-accordionTab header="Detailed Results ({{ evaluation.detailed_results.length }} test cases)">
                  @for (detail of evaluation.detailed_results; track detail.test_case_index) {
                    <div class="border-1 border-200 border-round p-3 mb-2">
                      <div class="flex align-items-start gap-3 mb-2">
                        <span class="text-xs text-500 font-mono bg-surface-100 px-2 py-1 border-round">#{{ detail.test_case_index }}</span>
                        <div class="flex-1">
                          <div class="text-xs text-500 mb-1">Input</div>
                          <div class="text-sm mb-2">
                            @if (isConversational(detail.input)) {
                              @for (turn of asConversation(detail.input); track $index) {
                                <div class="flex gap-2 mb-1" [class.justify-content-end]="turn.role === 'user'">
                                  <div
                                    class="text-sm p-2 border-round max-w-30rem"
                                    [class.bg-primary-100]="turn.role === 'user'"
                                    [class.bg-surface-100]="turn.role === 'assistant'"
                                  >
                                    <span class="text-xs font-semibold text-500 block mb-1">{{ turn.role }}</span>
                                    {{ turn.content }}
                                  </div>
                                </div>
                              }
                            } @else {
                              {{ detail.input }}
                            }
                          </div>
                          <div class="text-xs text-500 mb-1">Output</div>
                          <div class="text-sm text-700 bg-surface-50 p-2 border-round" style="max-height: 100px; overflow-y: auto;">
                            {{ helper.truncateOutput(detail.output) }}
                          </div>
                        </div>
                      </div>
                      <p-divider></p-divider>
                      <div class="flex flex-wrap gap-3">
                        @for (metricEntry of getDetailedMetricsEntries(detail.metrics); track metricEntry.key) {
                          <div class="flex align-items-center gap-2 bg-surface-50 px-2 py-1 border-round">
                            @if (metricEntry.value.success) {
                              <i-lucide [img]="CheckCircle" class="w-1rem h-1rem text-green-500"></i-lucide>
                            } @else {
                              <i-lucide [img]="XCircle" class="w-1rem h-1rem text-red-500"></i-lucide>
                            }
                            <span class="text-xs font-medium">{{ metricEntry.key }}:</span>
                            <span class="text-xs font-semibold" [class]="helper.getScoreClass(metricEntry.value.score)">
                              {{ metricEntry.value.score | number: '1.2-2' }}
                            </span>
                            @if (metricEntry.value.reason) {
                              <i-lucide
                                [img]="MessageSquare"
                                class="w-1rem h-1rem text-400 cursor-pointer"
                                [pTooltip]="metricEntry.value.reason"
                                tooltipPosition="top"
                              ></i-lucide>
                            }
                          </div>
                        }
                      </div>
                    </div>
                  }
                </p-accordionTab>
              </p-accordion>
            }
          </div>
        }
      }
    }
  `,
})
export class EvalQualityTabComponent {
  @Input() result: IEvaluationResult | null = null;
  @Input() mode: 'quality' | 'conversational' = 'quality';

  helper = inject(EvaluationResultsHelperService);

  protected readonly CheckCircle = CheckCircle;
  protected readonly XCircle = XCircle;
  protected readonly MessageSquare = MessageSquare;

  filterEvaluations(evaluations?: IEvaluation[]): IEvaluation[] {
    if (!evaluations) return [];
    const targetType = this.mode === 'conversational' ? 'multi_turn' : 'single_turn';
    return evaluations.filter(e => e.dataset_type === targetType);
  }

  isConversational(input: any): boolean {
    return Array.isArray(input);
  }

  asConversation(input: any): ConversationTurn[] {
    return input;
  }

  getDetailedMetricsEntries(metrics: Record<string, any>): { key: string; value: any }[] {
    return metrics ? Object.entries(metrics).map(([key, value]) => ({ key, value })) : [];
  }

  getSuccessRateBarClass(rate: number): string {
    if (rate >= 0.8) return 'success-high';
    if (rate >= 0.5) return 'success-medium';
    return 'success-low';
  }
}
