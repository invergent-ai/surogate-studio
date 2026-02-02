// src/app/shared/components/evaluation-results/components/eval-analytics-tab.component.ts
import { Component, inject, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartModule } from 'primeng/chart';
import { IEvaluationResult } from '../../../model/evaluation-result.model';
import { EvaluationResultsHelperService } from '../evaluation-results-helper.service';

@Component({
  standalone: true,
  selector: 'sm-eval-analytics-tab',
  imports: [CommonModule, ChartModule],
  template: `
    <div class="grid">
      <!-- Security Overview - Doughnut -->
      @if (helper.hasSecurityResults(result)) {
        <div class="col-12 md:col-6">
          <div class="border-1 border-200 border-round p-3">
            <h4 class="text-sm font-semibold mb-3">Security Overview</h4>
            <p-chart type="doughnut" [data]="securityDoughnutData" [options]="doughnutOptions" height="250"></p-chart>
          </div>
        </div>

        <!-- Vulnerability Defense Rates - Horizontal Bar -->
        <div class="col-12 md:col-6">
          <div class="border-1 border-200 border-round p-3">
            <h4 class="text-sm font-semibold mb-3">Defense Rate by Vulnerability</h4>
            <p-chart type="bar" [data]="vulnerabilityBarData" [options]="horizontalBarOptions" height="250"></p-chart>
          </div>
        </div>
      }

      <!-- Benchmark Scores - Bar -->
      @if (helper.hasBenchmarks(result)) {
        <div class="col-12 md:col-6">
          <div class="border-1 border-200 border-round p-3">
            <h4 class="text-sm font-semibold mb-3">Benchmark Scores</h4>
            <p-chart type="bar" [data]="benchmarkBarData" [options]="barOptions" height="250"></p-chart>
          </div>
        </div>
      }

      <!-- Conversation Metrics - Radar -->
      @if (helper.hasConversationalMetrics(result) && helper.getConversationalMetrics(result).length > 1) {
        <div class="col-12 md:col-6">
          <div class="border-1 border-200 border-round p-3">
            <h4 class="text-sm font-semibold mb-3">Conversation Metrics</h4>
            <p-chart type="radar" [data]="conversationRadarData" [options]="radarOptions" height="250"></p-chart>
          </div>
        </div>
      }

      <!-- Custom Evaluation - Bar -->
      @if (helper.hasCustomEvaluation(result)) {
        <div class="col-12 md:col-6">
          <div class="border-1 border-200 border-round p-3">
            <h4 class="text-sm font-semibold mb-3">Custom Evaluation by Task</h4>
            <p-chart type="bar" [data]="customEvalBarData" [options]="barOptions" height="250"></p-chart>
          </div>
        </div>

        <!-- Custom Evaluation Success/Fail - Pie -->
        <div class="col-12 md:col-6">
          <div class="border-1 border-200 border-round p-3">
            <h4 class="text-sm font-semibold mb-3">Custom Evaluation Results</h4>
            <p-chart type="pie" [data]="customEvalPieData" [options]="pieOptions" height="250"></p-chart>
          </div>
        </div>
      }
    </div>
  `,
})
export class EvalAnalyticsTabComponent implements OnChanges {
  @Input() result: IEvaluationResult | null = null;

  helper = inject(EvaluationResultsHelperService);

  // Chart data
  securityDoughnutData: any;
  vulnerabilityBarData: any;
  benchmarkBarData: any;
  conversationRadarData: any;
  customEvalBarData: any;
  customEvalPieData: any;

  // Chart options
  doughnutOptions = {
    plugins: {
      legend: { position: 'bottom', labels: { usePointStyle: true } },
    },
    cutout: '60%',
  };

  pieOptions = {
    plugins: {
      legend: { position: 'bottom', labels: { usePointStyle: true } },
    },
  };

  barOptions = {
    plugins: { legend: { display: false } },
    scales: {
      y: { beginAtZero: true, max: 1, ticks: { callback: (v: number) => `${(v * 100).toFixed(0)}%` } },
    },
  };

  horizontalBarOptions = {
    indexAxis: 'y' as const,
    plugins: { legend: { display: false } },
    scales: {
      x: { beginAtZero: true, max: 100, ticks: { callback: (v: number) => `${v}%` } },
    },
  };

  radarOptions = {
    plugins: { legend: { display: false } },
    scales: {
      r: { beginAtZero: true, max: 1, ticks: { stepSize: 0.2 } },
    },
  };

  ngOnChanges(changes: SimpleChanges) {
    if (changes['result'] && this.result) {
      this.buildCharts();
    }
  }

  private buildCharts() {
    this.buildSecurityCharts();
    this.buildBenchmarkChart();
    this.buildConversationChart();
    this.buildCustomEvalCharts();
  }

  private buildSecurityCharts() {
    if (!this.helper.hasSecurityResults(this.result)) return;

    const blocked = this.helper.getBlockedAttacks(this.result);
    const succeeded = this.helper.getSuccessfulAttacks(this.result);

    this.securityDoughnutData = {
      labels: ['Blocked', 'Succeeded'],
      datasets: [
        {
          data: [blocked, succeeded],
          backgroundColor: ['#22c55e', '#ef4444'],
        },
      ],
    };

    const vulnerabilities = this.getVulnerabilities();
    this.vulnerabilityBarData = {
      labels: vulnerabilities.map(v => this.helper.formatVulnerabilityType(v.vulnerability_type)),
      datasets: [
        {
          data: vulnerabilities.map(v => this.helper.getDefenseRate(v)),
          backgroundColor: vulnerabilities.map(v => {
            const rate = this.helper.getDefenseRate(v);
            return rate >= 80 ? '#22c55e' : rate >= 50 ? '#f59e0b' : '#ef4444';
          }),
        },
      ],
    };
  }

  private buildBenchmarkChart() {
    if (!this.helper.hasBenchmarks(this.result)) return;

    const benchmarks = this.getBenchmarks();
    this.benchmarkBarData = {
      labels: benchmarks.map(b => b.benchmark_name),
      datasets: [
        {
          data: benchmarks.map(b => b.overall_score),
          backgroundColor: '#6366f1',
        },
      ],
    };
  }

  private buildConversationChart() {
    if (!this.helper.hasConversationalMetrics(this.result)) return;

    const metrics = this.helper.getConversationalMetrics(this.result);
    if (metrics.length === 0) return;

    this.conversationRadarData = {
      labels: metrics.map(m => m.key),
      datasets: [
        {
          data: metrics.map(m => m.value.avg_score),
          backgroundColor: 'rgba(99, 102, 241, 0.2)',
          borderColor: '#6366f1',
          pointBackgroundColor: '#6366f1',
        },
      ],
    };
  }

  private buildCustomEvalCharts() {
    if (!this.helper.hasCustomEvaluation(this.result)) return;

    const tasks = this.helper.getCustomTaskSummary(this.result);
    this.customEvalBarData = {
      labels: tasks.map(t => t.name),
      datasets: [
        {
          data: tasks.map(t => t.score),
          backgroundColor: '#8b5cf6',
        },
      ],
    };

    const totalCorrect = tasks.reduce((sum, t) => sum + t.correct, 0);
    const totalWrong = tasks.reduce((sum, t) => sum + (t.total - t.correct), 0);
    this.customEvalPieData = {
      labels: ['Correct', 'Incorrect'],
      datasets: [
        {
          data: [totalCorrect, totalWrong],
          backgroundColor: ['#22c55e', '#ef4444'],
        },
      ],
    };
  }

  private getVulnerabilities(): any[] {
    return this.result?.targets?.flatMap(t => t.red_teaming?.vulnerabilities || []) || [];
  }

  private getBenchmarks(): any[] {
    return this.result?.targets?.flatMap(t => this.helper.getNonCustomBenchmarks(t.benchmarks || [])) || [];
  }
}
