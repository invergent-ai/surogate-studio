// src/app/shared/components/evaluation-results/evaluation-results.component.ts
import { Component, inject, Input, OnChanges, OnInit, signal, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TabViewModule } from 'primeng/tabview';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { BarChart3, FileText, LucideAngularModule } from 'lucide-angular';
import { CardComponent } from '../card/card.component';
import { EvalSummaryCardComponent } from './components/eval-summary-card.component';
import { EvalCustomTabComponent } from './components/eval-custom-tab.component';
import { EvalQualityTabComponent } from './components/eval-quality-tab.component';
import { EvalBenchmarksTabComponent } from './components/eval-benchmarks-tab.component';
import { EvalSecurityTabComponent } from './components/eval-security-tab.component';
import { EvalReportTabComponent } from './components/eval-report-tab.component';
import { EvaluationResultsHelperService } from './evaluation-results-helper.service';
import { EvaluationResultService } from '../../service/evaluation-result.service';
import { IEvaluationResult } from '../../model/evaluation-result.model';
import { EvalAnalyticsTabComponent } from './components/eval-analytics-tab.component';


@Component({
  standalone: true,
  selector: 'sm-evaluation-results',
  imports: [
    CommonModule,
    FormsModule,
    TabViewModule,
    ProgressSpinnerModule,
    LucideAngularModule,
    CardComponent,
    EvalSummaryCardComponent,
    EvalCustomTabComponent,
    EvalQualityTabComponent,
    EvalBenchmarksTabComponent,
    EvalSecurityTabComponent,
    EvalReportTabComponent,
    EvalAnalyticsTabComponent,
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
          <p-tabPanel header="Summary">
            <sm-eval-summary-card [result]="result()"></sm-eval-summary-card>
          </p-tabPanel>

          <p-tabPanel header="Analytics">
            <sm-eval-analytics-tab [result]="result()"></sm-eval-analytics-tab>
          </p-tabPanel>

          @if (helper.hasCustomEvaluation(result())) {
            <p-tabPanel header="Custom Evaluation">
              <sm-eval-custom-tab [result]="result()"></sm-eval-custom-tab>
            </p-tabPanel>
          }

          @if (helper.hasQualityMetrics(result())) {
            <p-tabPanel header="Quality">
              <sm-eval-quality-tab [result]="result()"></sm-eval-quality-tab>
            </p-tabPanel>
          }

          @if (helper.hasBenchmarks(result())) {
            <p-tabPanel header="Benchmarks">
              <sm-eval-benchmarks-tab [result]="result()"></sm-eval-benchmarks-tab>
            </p-tabPanel>
          }

          @if (helper.hasSecurityResults(result())) {
            <p-tabPanel header="Security">
              <sm-eval-security-tab [result]="result()"></sm-eval-security-tab>
            </p-tabPanel>
          }

          <p-tabPanel header="Report">
            <sm-eval-report-tab [taskRunId]="taskRunId" [report]="report()"></sm-eval-report-tab>
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
})
export class EvaluationResultsComponent implements OnInit, OnChanges {
  @Input() taskRunId?: string;

  private resultService = inject(EvaluationResultService);
  helper = inject(EvaluationResultsHelperService);

  loading = signal(false);
  error = signal<string | null>(null);
  result = signal<IEvaluationResult | null>(null);
  report = signal<string | null>(null);

  protected readonly BarChart3 = BarChart3;
  protected readonly FileText = FileText;

  ngOnInit() {
    if (this.taskRunId) this.loadResultByJobId();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['taskRunId'] && this.taskRunId) this.loadResultByJobId();
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
}
