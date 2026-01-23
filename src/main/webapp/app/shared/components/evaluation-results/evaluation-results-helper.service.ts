// src/app/shared/components/evaluation-results/evaluation-results-helper.service.ts
import { Injectable } from '@angular/core';
import {
  IEvaluationResult,
  IMetricSummary,
  IRedTeamingResult,
  ITaskResult,
  IVulnerabilityResult,
} from '../../model/evaluation-result.model';
import { ICustomBenchmark, ICustomTaskResult } from '../../model/evaluation-results.models';

@Injectable({ providedIn: 'root' })
export class EvaluationResultsHelperService {
  // ============================================================================
  // CUSTOM EVALUATION
  // ============================================================================

  hasCustomEvaluation(result: IEvaluationResult | null): boolean {
    return result?.targets?.some(t => t.benchmarks?.some(b => b.backend === 'custom_eval' || b.benchmark_name === 'custom')) || false;
  }

  getCustomBenchmarks(result: IEvaluationResult | null): ICustomBenchmark[] {
    const benchmarks: ICustomBenchmark[] = [];
    result?.targets?.forEach(t => {
      t.benchmarks?.forEach(b => {
        if (b.backend === 'custom_eval' || b.benchmark_name === 'custom') {
          benchmarks.push(b as unknown as ICustomBenchmark);
        }
      });
    });
    return benchmarks;
  }

  getNonCustomBenchmarks(benchmarks: any[]): any[] {
    return benchmarks?.filter(b => b.backend !== 'custom_eval' && b.benchmark_name !== 'custom') || [];
  }

  getCustomOverallScore(result: IEvaluationResult | null): number {
    const benchmarks = this.getCustomBenchmarks(result);
    if (benchmarks.length === 0) return 0;
    return benchmarks.reduce((sum, b) => sum + b.overall_score, 0) / benchmarks.length;
  }

  getCustomTaskSummary(result: IEvaluationResult | null): { name: string; score: number; correct: number; total: number }[] {
    const summary: { name: string; score: number; correct: number; total: number }[] = [];
    const benchmarks = this.getCustomBenchmarks(result);

    benchmarks.forEach(b => {
      if (b.task_results) {
        Object.entries(b.task_results).forEach(([key, value]) => {
          const task = value as ICustomTaskResult;
          if (task.accuracy !== undefined) {
            summary.push({ name: key.replace('_', ' '), score: task.accuracy, correct: task.correct || 0, total: task.total || 0 });
          } else if (task.avg_score !== undefined) {
            summary.push({
              name: key,
              score: task.avg_score,
              correct: Math.round((task.success_rate || 0) * (task.total || 0)),
              total: task.total || 0,
            });
          }
        });
      }
    });
    return summary;
  }

  // ============================================================================
  // QUALITY METRICS
  // ============================================================================

  hasQualityMetrics(result: IEvaluationResult | null): boolean {
    return result?.targets?.some(t => t.evaluations?.some(e => e.metrics_summary && Object.keys(e.metrics_summary).length > 0)) || false;
  }

  getTotalEvaluations(result: IEvaluationResult | null): number {
    let count = 0;
    result?.targets?.forEach(t => {
      t.evaluations?.forEach(e => {
        if (e.metrics_summary) count += Object.keys(e.metrics_summary).length;
      });
    });
    return count;
  }

  getOverallAvgScore(result: IEvaluationResult | null): number {
    let totalScore = 0,
      count = 0;
    result?.targets?.forEach(t => {
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

  getOverallSuccessRate(result: IEvaluationResult | null): number {
    let totalRate = 0,
      count = 0;
    result?.targets?.forEach(t => {
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

  // ============================================================================
  // BENCHMARKS
  // ============================================================================

  hasBenchmarks(result: IEvaluationResult | null): boolean {
    return result?.targets?.some(t => t.benchmarks?.some(b => b.backend !== 'custom_eval' && b.benchmark_name !== 'custom')) || false;
  }

  getTotalBenchmarks(result: IEvaluationResult | null): number {
    let count = 0;
    result?.targets?.forEach(t => {
      count += this.getNonCustomBenchmarks(t.benchmarks || []).length;
    });
    return count;
  }

  getPassedCount(result: IEvaluationResult | null): number {
    let count = 0;
    result?.targets?.forEach(t => {
      this.getNonCustomBenchmarks(t.benchmarks || []).forEach(b => {
        const status = b.status?.toLowerCase();
        if (['completed', 'success', 'passed'].includes(status)) count++;
      });
    });
    return count;
  }

  getFailedCount(result: IEvaluationResult | null): number {
    let count = 0;
    result?.targets?.forEach(t => {
      this.getNonCustomBenchmarks(t.benchmarks || []).forEach(b => {
        const status = b.status?.toLowerCase();
        if (['failed', 'error'].includes(status)) count++;
      });
    });
    return count;
  }

  // ============================================================================
  // SECURITY
  // ============================================================================

  hasSecurityResults(result: IEvaluationResult | null): boolean {
    return result?.targets?.some(t => t.red_teaming?.vulnerabilities && t.red_teaming.vulnerabilities.length > 0) || false;
  }

  getTotalVulnerabilities(result: IEvaluationResult | null): number {
    let count = 0;
    result?.targets?.forEach(t => {
      if (t.red_teaming?.vulnerabilities) count += t.red_teaming.vulnerabilities.length;
    });
    return count;
  }

  getTotalAttacks(result: IEvaluationResult | null): number {
    let count = 0;
    result?.targets?.forEach(t => {
      t.red_teaming?.vulnerabilities?.forEach(v => {
        count += v.total_attacks || 0;
      });
    });
    return count;
  }

  getSuccessfulAttacks(result: IEvaluationResult | null): number {
    let count = 0;
    result?.targets?.forEach(t => {
      t.red_teaming?.vulnerabilities?.forEach(v => {
        count += v.successful_attacks || 0;
      });
    });
    return count;
  }

  getBlockedAttacks(result: IEvaluationResult | null): number {
    let count = 0;
    result?.targets?.forEach(t => {
      t.red_teaming?.vulnerabilities?.forEach(v => {
        count += v.failed_attacks || 0;
      });
    });
    return count;
  }

  getSecurityScore(result: IEvaluationResult | null): number {
    const total = this.getTotalAttacks(result);
    if (total === 0) return 100;
    return (this.getBlockedAttacks(result) / total) * 100;
  }

  getDefenseRate(vuln: IVulnerabilityResult): number {
    if (!vuln.total_attacks || vuln.total_attacks === 0) return 100;
    return ((vuln.failed_attacks || 0) / vuln.total_attacks) * 100;
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

  // ============================================================================
  // UTILITIES
  // ============================================================================

  getScoreClass(score: number): string {
    if (score >= 0.8) return 'text-green-500';
    if (score >= 0.5) return 'text-yellow-500';
    return 'text-red-500';
  }

  getStatusSeverity(status?: string): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    const s = status?.toLowerCase();
    if (['completed', 'success', 'passed'].includes(s || '')) return 'success';
    if (['failed', 'error'].includes(s || '')) return 'danger';
    if (['running', 'pending'].includes(s || '')) return 'warning';
    return 'secondary';
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

  formatVulnerabilityType(type: string): string {
    if (!type) return 'Unknown';
    return type
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  truncateOutput(output: string, maxLength: number = 200): string {
    if (!output) return '';
    if (output.length <= maxLength) return output;
    return output.substring(0, maxLength) + '...';
  }

  getMetricsSummaryEntries(metricsSummary: { [key: string]: IMetricSummary }): { key: string; value: IMetricSummary }[] {
    if (!metricsSummary) return [];
    return Object.entries(metricsSummary).map(([key, value]) => ({ key, value }));
  }

  getTaskResultEntries(taskResults: { [key: string]: ITaskResult }): { name: string; result: ITaskResult }[] {
    if (!taskResults) return [];
    return Object.entries(taskResults).map(([name, result]) => ({ name, result }));
  }

  getCustomTaskEntries(taskResults: { [key: string]: ICustomTaskResult }): { key: string; value: ICustomTaskResult }[] {
    if (!taskResults) return [];
    return Object.entries(taskResults).map(([key, value]) => ({ key, value }));
  }
}
