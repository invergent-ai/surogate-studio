import { Component, Input, OnInit, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { EvaluationResultService } from '../../service/evaluation-result.service';
import {
  IEvaluationResult,
  ITaskResult,
  IRedTeamingResult,
  IVulnerabilityResult,
  IEvaluation,
  IDetailedResult,
  IMetricSummary,
} from '../../model/evaluation-result.model';
import { CommonModule } from '@angular/common';
import { CardComponent } from '../card/card.component';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonDirective } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { FormsModule } from '@angular/forms';
import { TabViewModule } from 'primeng/tabview';
import { LucideAngularModule, FileText, BarChart3, ShieldCheck, ShieldAlert, CheckCircle, XCircle, MessageSquare } from 'lucide-angular';
import { MarkdownModule } from 'ngx-markdown';
import { ProgressBarModule } from 'primeng/progressbar';
import { DividerModule } from 'primeng/divider';
import { AccordionModule } from 'primeng/accordion';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  standalone: true,
  selector: 'sm-evaluation-results',
  imports: [
    CommonModule,
    CardComponent,
    TableModule,
    TagModule,
    ButtonDirective,
    ProgressSpinnerModule,
    FormsModule,
    TabViewModule,
    LucideAngularModule,
    MarkdownModule,
    ProgressBarModule,
    DividerModule,
    AccordionModule,
    TooltipModule,
  ],
  template: `
    <sm-card header="Evaluation Results" [icon]="BarChart3">
      @if (loading()) {
        <div class="flex justify-content-center p-4">
          <p-progressSpinner [style]="{ width: '40px', height: '40px' }"></p-progressSpinner>
        </div>
      } @else if (error()) {
        <div class="text-center text-500 p-4">
          <p>{{ error() }}</p>
        </div>
      } @else if (result()) {
        <p-tabView>
          <!-- Summary Tab -->
          <p-tabPanel header="Summary">
            @if (hasSecurityResults()) {
              <!-- Security Score Card -->
              <div class="border-1 border-200 border-round p-4 mb-4">
                <div class="flex flex-column align-items-center mb-4">
                  <i-lucide [img]="ShieldCheck" class="w-3rem h-3rem mb-2" [class]="getSecurityScoreClass()"></i-lucide>
                  <div class="text-sm text-500 mb-1">Security Score</div>
                  <div class="text-4xl font-bold mb-2" [class]="getSecurityScoreClass()">{{ getSecurityScore() | number: '1.0-0' }}%</div>
                  <p-tag [severity]="getOverallSecuritySeverity()" [value]="getOverallSecurityLabel()"></p-tag>
                </div>
                <p-divider></p-divider>
                <div class="grid text-center">
                  <div class="col-3">
                    <div class="text-2xl font-semibold">{{ getTotalVulnerabilities() }}</div>
                    <div class="text-xs text-500">Vulnerabilities</div>
                  </div>
                  <div class="col-3">
                    <div class="text-2xl font-semibold">{{ getTotalAttacks() }}</div>
                    <div class="text-xs text-500">Total Attacks</div>
                  </div>
                  <div class="col-3">
                    <div class="text-2xl font-semibold text-green-500">{{ getBlockedAttacks() }}</div>
                    <div class="text-xs text-500">Blocked</div>
                  </div>
                  <div class="col-3">
                    <div class="text-2xl font-semibold text-red-500">{{ getSuccessfulAttacks() }}</div>
                    <div class="text-xs text-500">Succeeded</div>
                  </div>
                </div>
              </div>
            }

            @if (hasQualityMetrics()) {
              <!-- Quality Metrics Summary Card -->
              <div class="border-1 border-200 border-round p-4 mb-4">
                <div class="flex align-items-center gap-3 mb-3">
                  <i-lucide [img]="CheckCircle" class="w-1.5rem h-1.5rem text-primary"></i-lucide>
                  <span class="font-semibold">Quality Metrics</span>
                </div>
                <div class="grid text-center">
                  <div class="col-4">
                    <div class="text-2xl font-semibold text-primary">{{ getTotalEvaluations() }}</div>
                    <div class="text-xs text-500">Evaluations</div>
                  </div>
                  <div class="col-4">
                    <div class="text-2xl font-semibold" [class]="getAvgScoreClass()">{{ getOverallAvgScore() | number: '1.2-2' }}</div>
                    <div class="text-xs text-500">Avg Score</div>
                  </div>
                  <div class="col-4">
                    <div class="text-2xl font-semibold" [class]="getSuccessRateClass()">
                      {{ getOverallSuccessRate() | percent: '1.0-0' }}
                    </div>
                    <div class="text-xs text-500">Success Rate</div>
                  </div>
                </div>
              </div>
            }

            @if (hasBenchmarks()) {
              <!-- Benchmarks Summary Card -->
              <div class="border-1 border-200 border-round p-4 mb-4">
                <div class="flex align-items-center gap-3 mb-3">
                  <i-lucide [img]="BarChart3" class="w-1.5rem h-1.5rem text-primary"></i-lucide>
                  <span class="font-semibold">Benchmarks</span>
                </div>
                <div class="grid text-center">
                  <div class="col-4">
                    <div class="text-2xl font-semibold text-primary">{{ getTotalBenchmarks() }}</div>
                    <div class="text-xs text-500">Total</div>
                  </div>
                  <div class="col-4">
                    <div class="text-2xl font-semibold text-green-500">{{ getPassedCount() }}</div>
                    <div class="text-xs text-500">Completed</div>
                  </div>
                  <div class="col-4">
                    <div class="text-2xl font-semibold text-red-500">{{ getFailedCount() }}</div>
                    <div class="text-xs text-500">Failed</div>
                  </div>
                </div>
              </div>
            }

            <!-- Project Info Card -->
            <div class="border-1 border-200 border-round p-4">
              <div class="flex align-items-center gap-3 mb-3">
                <i-lucide [img]="FileText" class="w-1.5rem h-1.5rem text-500"></i-lucide>
                <span class="font-semibold">Project Info</span>
              </div>
              <div class="grid">
                <div class="col-4">
                  <div class="text-xs text-500 mb-1">Name</div>
                  <div class="font-medium">{{ result()?.project?.name }}</div>
                </div>
                <div class="col-4">
                  <div class="text-xs text-500 mb-1">Model</div>
                  <div class="font-medium">{{ getModelUnderTest()?.model }}</div>
                </div>
                <div class="col-4">
                  <div class="text-xs text-500 mb-1">Timestamp</div>
                  <div class="font-medium">{{ result()?.timestamp | date: 'medium' }}</div>
                </div>
              </div>
            </div>
          </p-tabPanel>

          <!-- Quality Metrics Tab -->
          @if (hasQualityMetrics()) {
            <p-tabPanel header="Quality">
              @for (target of result()?.targets; track target.name) {
                @if (target.evaluations && target.evaluations.length > 0) {
                  @for (evaluation of target.evaluations; track evaluation.name) {
                    @if (evaluation.metrics_summary) {
                      <div class="mb-4">
                        <div class="flex align-items-center justify-content-between mb-3">
                          <div>
                            <h4 class="text-sm font-semibold mb-1">{{ evaluation.name }}</h4>
                            <span class="text-xs text-500">{{ evaluation.dataset }} · {{ evaluation.num_test_cases }} test cases</span>
                          </div>
                          <p-tag [severity]="getEvalStatusSeverity(evaluation.status)" [value]="evaluation.status || 'unknown'"></p-tag>
                        </div>

                        <!-- Metrics Summary Table -->
                        <p-table [value]="getMetricsSummaryEntries(evaluation.metrics_summary)" styleClass="p-datatable-sm mb-3">
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
                                <span class="font-semibold" [class]="getScoreClass(metric.value.avg_score)">
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

                        <!-- Detailed Results Accordion -->
                        @if (evaluation.detailed_results && evaluation.detailed_results.length > 0) {
                          <p-accordion>
                            <p-accordionTab header="Detailed Results ({{ evaluation.detailed_results.length }} test cases)">
                              @for (detail of evaluation.detailed_results; track detail.test_case_index) {
                                <div class="border-1 border-200 border-round p-3 mb-2">
                                  <div class="flex align-items-start gap-3 mb-2">
                                    <span class="text-xs text-500 font-mono bg-surface-100 px-2 py-1 border-round"
                                      >#{{ detail.test_case_index }}</span
                                    >
                                    <div class="flex-1">
                                      <div class="text-xs text-500 mb-1">Input</div>
                                      <div class="text-sm mb-2">{{ detail.input }}</div>
                                      <div class="text-xs text-500 mb-1">Output</div>
                                      <div
                                        class="text-sm text-700 bg-surface-50 p-2 border-round"
                                        style="max-height: 100px; overflow-y: auto;"
                                      >
                                        {{ truncateOutput(detail.output) }}
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
                                        <span class="text-xs font-semibold" [class]="getScoreClass(metricEntry.value.score)">
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
              }
            </p-tabPanel>
          }

          <!-- Benchmarks Tab -->
          @if (hasBenchmarks()) {
            <p-tabPanel header="Benchmarks">
              @for (target of result()?.targets; track target.name) {
                @if (target.benchmarks && target.benchmarks.length > 0) {
                  <h4 class="text-sm font-semibold mb-2">{{ target.name }} ({{ target.model }})</h4>
                  <p-table [value]="target.benchmarks" styleClass="p-datatable-sm">
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
                          <p-tag [severity]="getBenchmarkStatusSeverity(benchmark.status)" [value]="benchmark.status"></p-tag>
                        </td>
                      </tr>
                    </ng-template>
                  </p-table>

                  @for (benchmark of target.benchmarks; track benchmark.benchmark_name) {
                    @if (benchmark.task_results && getTaskResultEntries(benchmark.task_results).length > 0) {
                      <div class="mt-3 mb-4">
                        <h5 class="text-xs font-semibold mb-2 text-500">{{ benchmark.benchmark_name }} - Task Breakdown</h5>
                        <p-table [value]="getTaskResultEntries(benchmark.task_results)" styleClass="p-datatable-sm">
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
            </p-tabPanel>
          }

          <!-- Security Tab -->
          @if (hasSecurityResults()) {
            <p-tabPanel header="Security">
              @for (target of result()?.targets; track target.name) {
                @if (target.red_teaming && target.red_teaming.vulnerabilities?.length > 0) {
                  <div class="mb-4">
                    <p-table [value]="target.red_teaming.vulnerabilities" styleClass="p-datatable-sm">
                      <ng-template pTemplate="header">
                        <tr>
                          <th>Vulnerability</th>
                          <th class="text-center">Attacks</th>
                          <th class="text-center">Blocked</th>
                          <th class="text-center">Succeeded</th>
                          <th>Defense Rate</th>
                          <th>Severity</th>
                        </tr>
                      </ng-template>
                      <ng-template pTemplate="body" let-vuln>
                        <tr>
                          <td>
                            <div>
                              <span class="font-medium">{{
                                vuln.vulnerability_name || formatVulnerabilityType(vuln.vulnerability_type)
                              }}</span>
                              <div class="text-xs text-500">{{ formatVulnerabilityType(vuln.vulnerability_type) }}</div>
                            </div>
                          </td>
                          <td class="text-center">{{ vuln.total_attacks }}</td>
                          <td class="text-center text-green-500 font-medium">{{ vuln.failed_attacks }}</td>
                          <td class="text-center text-red-500 font-medium">{{ vuln.successful_attacks }}</td>
                          <td style="width: 150px">
                            <div class="flex align-items-center gap-2">
                              <p-progressBar
                                [value]="getDefenseRate(vuln)"
                                [showValue]="false"
                                [style]="{ width: '80px', height: '6px' }"
                                [styleClass]="getDefenseRateClass(vuln)"
                              ></p-progressBar>
                              <span class="text-xs font-medium">{{ getDefenseRate(vuln) | number: '1.0-0' }}%</span>
                            </div>
                          </td>
                          <td>
                            <p-tag [severity]="getSeverityTagSeverity(vuln.severity)" [value]="vuln.severity" [rounded]="true"></p-tag>
                          </td>
                        </tr>
                      </ng-template>
                    </p-table>

                    <div class="mt-4">
                      <h5 class="text-xs font-semibold mb-2 text-500">Attack Methods Used</h5>
                      <div class="flex flex-wrap gap-2">
                        @for (attack of getAllAttackMethods(target.red_teaming); track attack.method) {
                          <p-tag severity="secondary" [rounded]="true">{{ attack.method }}: {{ attack.count }}</p-tag>
                        }
                      </div>
                    </div>
                  </div>
                }
              }
            </p-tabPanel>
          }

          <!-- Report Tab -->
          <p-tabPanel header="Report">
            @if (report()) {
              <div class="markdown-content border-1 border-200 border-round p-3 overflow-auto" style="max-height: 400px;">
                <markdown [data]="report()"></markdown>
              </div>
            } @else {
              <div class="text-center text-500 p-4">No report available</div>
            }
          </p-tabPanel>
        </p-tabView>
      } @else {
        <div class="text-center text-500 p-4">
          <i-lucide [img]="FileText" class="w-3rem h-3rem mb-2 text-300"></i-lucide>
          <p>No evaluation results available</p>
        </div>
      }
    </sm-card>
  `,
  styles: [
    `
      .markdown-content {
        ::ng-deep {
          h1,
          h2,
          h3,
          h4,
          h5,
          h6 {
            margin-top: 1rem;
            margin-bottom: 0.5rem;
            font-weight: 600;
          }
          h1 {
            font-size: 1.5rem;
          }
          h2 {
            font-size: 1.25rem;
          }
          h3 {
            font-size: 1.1rem;
          }
          p {
            margin-bottom: 0.75rem;
          }
          ul,
          ol {
            margin-left: 1.5rem;
            margin-bottom: 0.75rem;
          }
          li {
            margin-bottom: 0.25rem;
          }
          code {
            background: var(--surface-200);
            padding: 0.125rem 0.375rem;
            border-radius: 4px;
            font-size: 0.875rem;
          }
          pre {
            background: var(--surface-200);
            padding: 1rem;
            border-radius: 6px;
            overflow-x: auto;
            code {
              background: transparent;
              padding: 0;
            }
          }
          table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 1rem;
            th,
            td {
              border: 1px solid var(--surface-300);
              padding: 0.5rem;
              text-align: left;
            }
            th {
              background: var(--surface-200);
              font-weight: 600;
            }
          }
        }
      }

      ::ng-deep .defense-high .p-progressbar-value {
        background: var(--green-500);
      }
      ::ng-deep .defense-medium .p-progressbar-value {
        background: var(--yellow-500);
      }
      ::ng-deep .defense-low .p-progressbar-value {
        background: var(--red-500);
      }
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
})
export class EvaluationResultsComponent implements OnInit, OnChanges {
  @Input() taskRunId?: string;

  private resultService = inject(EvaluationResultService);

  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<IEvaluationResult | null>(null);
  report = signal<string | null>(null);

  protected readonly FileText = FileText;
  protected readonly BarChart3 = BarChart3;
  protected readonly ShieldCheck = ShieldCheck;
  protected readonly ShieldAlert = ShieldAlert;
  protected readonly CheckCircle = CheckCircle;
  protected readonly XCircle = XCircle;
  protected readonly MessageSquare = MessageSquare;

  ngOnInit() {
    if (this.taskRunId) {
      this.loadResultByJobId();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['taskRunId'] && this.taskRunId) {
      this.loadResultByJobId();
    }
  }

  loadResultByJobId() {
    if (!this.taskRunId) return;

    this.loading.set(true);
    this.error.set(null);

    this.resultService.getResultByJobId(this.taskRunId).subscribe({
      next: result => {
        if (result) {
          this.result.set(result);
          this.loadReportByJobId();
        } else {
          this.error.set('No results found for this job');
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load results');
        this.loading.set(false);
      },
    });
  }

  loadReportByJobId() {
    if (!this.taskRunId) return;

    this.resultService.getReportByJobId(this.taskRunId).subscribe({
      next: report => this.report.set(report),
      error: () => this.report.set(null),
    });
  }

  getModelUnderTest(): any {
    return this.result()?.targets?.find(t => t.name === 'model-under-test');
  }

  // Quality Metrics methods
  hasQualityMetrics(): boolean {
    return (
      this.result()?.targets?.some(t => t.evaluations?.some(e => e.metrics_summary && Object.keys(e.metrics_summary).length > 0)) || false
    );
  }

  getTotalEvaluations(): number {
    let count = 0;
    this.result()?.targets?.forEach(t => {
      t.evaluations?.forEach(e => {
        if (e.metrics_summary) {
          count += Object.keys(e.metrics_summary).length;
        }
      });
    });
    return count;
  }

  getOverallAvgScore(): number {
    let totalScore = 0;
    let count = 0;
    this.result()?.targets?.forEach(t => {
      t.evaluations?.forEach(e => {
        if (e.metrics_summary) {
          Object.values(e.metrics_summary).forEach(m => {
            totalScore += m.avg_score;
            count++;
          });
        }
      });
    });
    return count > 0 ? totalScore / count : 0;
  }

  getOverallSuccessRate(): number {
    let totalRate = 0;
    let count = 0;
    this.result()?.targets?.forEach(t => {
      t.evaluations?.forEach(e => {
        if (e.metrics_summary) {
          Object.values(e.metrics_summary).forEach(m => {
            totalRate += m.success_rate;
            count++;
          });
        }
      });
    });
    return count > 0 ? totalRate / count : 0;
  }

  getMetricsSummaryEntries(metricsSummary: { [key: string]: IMetricSummary }): { key: string; value: IMetricSummary }[] {
    if (!metricsSummary) return [];
    return Object.entries(metricsSummary).map(([key, value]) => ({ key, value }));
  }

  getDetailedMetricsEntries(metrics: { [key: string]: any }): { key: string; value: any }[] {
    if (!metrics) return [];
    return Object.entries(metrics).map(([key, value]) => ({ key, value }));
  }

  truncateOutput(output: string, maxLength: number = 200): string {
    if (!output) return '';
    if (output.length <= maxLength) return output;
    return output.substring(0, maxLength) + '...';
  }

  getScoreClass(score: number): string {
    if (score >= 0.8) return 'text-green-500';
    if (score >= 0.5) return 'text-yellow-500';
    return 'text-red-500';
  }

  getAvgScoreClass(): string {
    return this.getScoreClass(this.getOverallAvgScore());
  }

  getSuccessRateClass(): string {
    const rate = this.getOverallSuccessRate();
    if (rate >= 0.8) return 'text-green-500';
    if (rate >= 0.5) return 'text-yellow-500';
    return 'text-red-500';
  }

  getSuccessRateBarClass(rate: number): string {
    if (rate >= 0.8) return 'success-high';
    if (rate >= 0.5) return 'success-medium';
    return 'success-low';
  }

  getEvalStatusSeverity(status?: string): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    const s = status?.toLowerCase();
    if (s === 'completed' || s === 'success') return 'success';
    if (s === 'failed' || s === 'error') return 'danger';
    if (s === 'running' || s === 'pending') return 'warning';
    return 'secondary';
  }

  // Benchmarks methods
  hasBenchmarks(): boolean {
    return this.result()?.targets?.some(t => t.benchmarks && t.benchmarks.length > 0) || false;
  }

  getTotalBenchmarks(): number {
    let count = 0;
    this.result()?.targets?.forEach(t => {
      count += t.benchmarks?.length || 0;
    });
    return count;
  }

  getPassedCount(): number {
    let count = 0;
    this.result()?.targets?.forEach(t => {
      t.benchmarks?.forEach(b => {
        const status = b.status?.toLowerCase();
        if (status === 'completed' || status === 'success' || status === 'passed') {
          count++;
        }
      });
    });
    return count;
  }

  getFailedCount(): number {
    let count = 0;
    this.result()?.targets?.forEach(t => {
      t.benchmarks?.forEach(b => {
        const status = b.status?.toLowerCase();
        if (status === 'failed' || status === 'error') {
          count++;
        }
      });
    });
    return count;
  }

  getBenchmarkStatusSeverity(status: string): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    const s = status?.toLowerCase();
    if (s === 'completed' || s === 'success' || s === 'passed') return 'success';
    if (s === 'failed' || s === 'error') return 'danger';
    if (s === 'running' || s === 'pending') return 'warning';
    return 'secondary';
  }

  getTaskResultEntries(taskResults: { [key: string]: ITaskResult }): { name: string; result: ITaskResult }[] {
    if (!taskResults) return [];
    return Object.entries(taskResults).map(([name, result]) => ({ name, result }));
  }

  // Security methods
  hasSecurityResults(): boolean {
    return (
      this.result()?.targets?.some(t => t.red_teaming && t.red_teaming.vulnerabilities && t.red_teaming.vulnerabilities.length > 0) || false
    );
  }

  getTotalVulnerabilities(): number {
    let count = 0;
    this.result()?.targets?.forEach(t => {
      if (t.red_teaming?.vulnerabilities) {
        count += t.red_teaming.vulnerabilities.length;
      }
    });
    return count;
  }

  getTotalAttacks(): number {
    let count = 0;
    this.result()?.targets?.forEach(t => {
      t.red_teaming?.vulnerabilities?.forEach(v => {
        count += v.total_attacks || 0;
      });
    });
    return count;
  }

  getSuccessfulAttacks(): number {
    let count = 0;
    this.result()?.targets?.forEach(t => {
      t.red_teaming?.vulnerabilities?.forEach(v => {
        count += v.successful_attacks || 0;
      });
    });
    return count;
  }

  getBlockedAttacks(): number {
    let count = 0;
    this.result()?.targets?.forEach(t => {
      t.red_teaming?.vulnerabilities?.forEach(v => {
        count += v.failed_attacks || 0;
      });
    });
    return count;
  }

  getSecurityScore(): number {
    const total = this.getTotalAttacks();
    if (total === 0) return 100;
    const blocked = this.getBlockedAttacks();
    return (blocked / total) * 100;
  }

  getSecurityScoreClass(): string {
    const score = this.getSecurityScore();
    if (score >= 80) return 'text-green-500';
    if (score >= 50) return 'text-yellow-500';
    return 'text-red-500';
  }

  getDefenseRate(vuln: IVulnerabilityResult): number {
    if (!vuln.total_attacks || vuln.total_attacks === 0) return 100;
    return ((vuln.failed_attacks || 0) / vuln.total_attacks) * 100;
  }

  getDefenseRateClass(vuln: IVulnerabilityResult): string {
    const rate = this.getDefenseRate(vuln);
    if (rate >= 80) return 'defense-high';
    if (rate >= 50) return 'defense-medium';
    return 'defense-low';
  }

  getSeverityTagSeverity(severity: string): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    switch (severity?.toLowerCase()) {
      case 'critical':
      case 'high':
        return 'danger';
      case 'medium':
        return 'warning';
      case 'low':
        return 'success';
      default:
        return 'secondary';
    }
  }

  getOverallSecuritySeverity(): 'success' | 'info' | 'warning' | 'danger' {
    const score = this.getSecurityScore();
    if (score >= 80) return 'success';
    if (score >= 50) return 'warning';
    return 'danger';
  }

  getOverallSecurityLabel(): string {
    const score = this.getSecurityScore();
    if (score >= 80) return 'Secure';
    if (score >= 50) return 'Moderate Risk';
    return 'High Risk';
  }

  formatVulnerabilityType(type: string): string {
    if (!type) return 'Unknown';
    return type
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  getAllAttackMethods(redTeaming: IRedTeamingResult): { method: string; count: number }[] {
    const attackCounts: { [key: string]: number } = {};

    redTeaming.vulnerabilities?.forEach(vuln => {
      if (vuln.attack_breakdown) {
        Object.entries(vuln.attack_breakdown).forEach(([method, count]) => {
          attackCounts[method] = (attackCounts[method] || 0) + count;
        });
      }
    });

    return Object.entries(attackCounts)
      .map(([method, count]) => ({ method, count }))
      .sort((a, b) => b.count - a.count);
  }
}
