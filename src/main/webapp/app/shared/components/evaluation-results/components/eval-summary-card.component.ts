// src/app/shared/components/evaluation-results/components/eval-summary-card.component.ts
import { Component, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';
import { BarChart3, CheckCircle, FileText, FlaskConical, LucideAngularModule, ShieldCheck } from 'lucide-angular';
import { IEvaluationResult } from '../../../model/evaluation-result.model';
import { EvaluationResultsHelperService } from '../evaluation-results-helper.service';

@Component({
  standalone: true,
  selector: 'sm-eval-summary-card',
  imports: [CommonModule, TagModule, DividerModule, LucideAngularModule],
  template: `
    <!-- Security Score Card -->
    @if (helper.hasSecurityResults(result)) {
      <div class="border-1 border-200 border-round p-4 mb-4">
        <div class="flex flex-column align-items-center mb-4">
          <i-lucide [img]="ShieldCheck" class="w-3rem h-3rem mb-2" [class]="getSecurityScoreClass()"></i-lucide>
          <div class="text-sm text-500 mb-1">Security Score</div>
          <div class="text-4xl font-bold mb-2" [class]="getSecurityScoreClass()">
            {{ helper.getSecurityScore(result) | number: '1.0-0' }}%
          </div>
          <p-tag [severity]="getOverallSecuritySeverity()" [value]="getOverallSecurityLabel()"></p-tag>
        </div>
        <p-divider></p-divider>
        <div class="grid text-center">
          <div class="col-3">
            <div class="text-2xl font-semibold">{{ helper.getTotalVulnerabilities(result) }}</div>
            <div class="text-xs text-500">Vulnerabilities</div>
          </div>
          <div class="col-3">
            <div class="text-2xl font-semibold">{{ helper.getTotalAttacks(result) }}</div>
            <div class="text-xs text-500">Total Attacks</div>
          </div>
          <div class="col-3">
            <div class="text-2xl font-semibold text-green-500">{{ helper.getBlockedAttacks(result) }}</div>
            <div class="text-xs text-500">Blocked</div>
          </div>
          <div class="col-3">
            <div class="text-2xl font-semibold text-red-500">{{ helper.getSuccessfulAttacks(result) }}</div>
            <div class="text-xs text-500">Succeeded</div>
          </div>
        </div>
      </div>
    }

    <!-- Custom Evaluation Summary Card -->
    @if (helper.hasCustomEvaluation(result)) {
      <div class="border-1 border-200 border-round p-4 mb-4">
        <div class="flex align-items-center gap-3 mb-3">
          <i-lucide [img]="FlaskConical" class="w-1.5rem h-1.5rem text-primary"></i-lucide>
          <span class="font-semibold">Custom Evaluation</span>
        </div>
        <div class="flex flex-column align-items-center mb-3">
          <div class="text-4xl font-bold mb-1" [class]="helper.getScoreClass(helper.getCustomOverallScore(result))">
            {{ helper.getCustomOverallScore(result) | percent: '1.1-1' }}
          </div>
          <div class="text-sm text-500">Overall Score</div>
        </div>
        <p-divider></p-divider>
        <div class="grid text-center">
          @for (taskEntry of helper.getCustomTaskSummary(result); track taskEntry.name) {
            <div class="col">
              <div class="text-2xl font-semibold" [class]="helper.getScoreClass(taskEntry.score)">
                {{ taskEntry.score | percent: '1.0-0' }}
              </div>
              <div class="text-xs text-500">{{ taskEntry.name | titlecase }}</div>
              <div class="text-xs text-400">{{ taskEntry.correct }}/{{ taskEntry.total }}</div>
            </div>
          }
        </div>
      </div>
    }

    <!-- Quality Metrics Summary Card -->
    @if (helper.hasQualityMetrics(result)) {
      <div class="border-1 border-200 border-round p-4 mb-4">
        <div class="flex align-items-center gap-3 mb-3">
          <i-lucide [img]="CheckCircle" class="w-1.5rem h-1.5rem text-primary"></i-lucide>
          <span class="font-semibold">Quality Metrics</span>
        </div>
        <div class="grid text-center">
          <div class="col-4">
            <div class="text-2xl font-semibold text-primary">{{ helper.getTotalEvaluations(result) }}</div>
            <div class="text-xs text-500">Evaluations</div>
          </div>
          <div class="col-4">
            <div class="text-2xl font-semibold" [class]="helper.getScoreClass(helper.getOverallAvgScore(result))">
              {{ helper.getOverallAvgScore(result) | number: '1.2-2' }}
            </div>
            <div class="text-xs text-500">Avg Score</div>
          </div>
          <div class="col-4">
            <div class="text-2xl font-semibold" [class]="helper.getScoreClass(helper.getOverallSuccessRate(result))">
              {{ helper.getOverallSuccessRate(result) | percent: '1.0-0' }}
            </div>
            <div class="text-xs text-500">Success Rate</div>
          </div>
        </div>
      </div>
    }

    <!-- Benchmarks Summary Card -->
    @if (helper.hasBenchmarks(result)) {
      <div class="border-1 border-200 border-round p-4 mb-4">
        <div class="flex align-items-center gap-3 mb-3">
          <i-lucide [img]="BarChart3" class="w-1.5rem h-1.5rem text-primary"></i-lucide>
          <span class="font-semibold">Benchmarks</span>
        </div>
        <div class="grid text-center">
          <div class="col-4">
            <div class="text-2xl font-semibold text-primary">{{ helper.getTotalBenchmarks(result) }}</div>
            <div class="text-xs text-500">Total</div>
          </div>
          <div class="col-4">
            <div class="text-2xl font-semibold text-green-500">{{ helper.getPassedCount(result) }}</div>
            <div class="text-xs text-500">Completed</div>
          </div>
          <div class="col-4">
            <div class="text-2xl font-semibold text-red-500">{{ helper.getFailedCount(result) }}</div>
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
          <div class="font-medium">{{ result?.project?.name }}</div>
        </div>
        <div class="col-4">
          <div class="text-xs text-500 mb-1">Model</div>
          <div class="font-medium">{{ getModelUnderTest()?.model }}</div>
        </div>
        <div class="col-4">
          <div class="text-xs text-500 mb-1">Timestamp</div>
          <div class="font-medium">{{ result?.timestamp | date: 'medium' }}</div>
        </div>
      </div>
    </div>
  `,
})
export class EvalSummaryCardComponent {
  @Input() result: IEvaluationResult | null = null;

  helper = inject(EvaluationResultsHelperService);

  protected readonly ShieldCheck = ShieldCheck;
  protected readonly FlaskConical = FlaskConical;
  protected readonly CheckCircle = CheckCircle;
  protected readonly BarChart3 = BarChart3;
  protected readonly FileText = FileText;

  getModelUnderTest(): any {
    return this.result?.targets?.find(t => t.name === 'model-under-test');
  }

  getSecurityScoreClass(): string {
    const score = this.helper.getSecurityScore(this.result);
    if (score >= 80) return 'text-green-500';
    if (score >= 50) return 'text-yellow-500';
    return 'text-red-500';
  }

  getOverallSecuritySeverity(): 'success' | 'warning' | 'danger' {
    const score = this.helper.getSecurityScore(this.result);
    if (score >= 80) return 'success';
    if (score >= 50) return 'warning';
    return 'danger';
  }

  getOverallSecurityLabel(): string {
    const score = this.helper.getSecurityScore(this.result);
    if (score >= 80) return 'Secure';
    if (score >= 50) return 'Moderate Risk';
    return 'High Risk';
  }
}
