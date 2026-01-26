// src/app/shared/components/evaluation-results/components/eval-security-tab.component.ts
import { Component, inject, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ProgressBarModule } from 'primeng/progressbar';
import { AccordionModule } from 'primeng/accordion';
import { TooltipModule } from 'primeng/tooltip';
import { CheckCircle, LucideAngularModule, MessageSquare, ShieldAlert, ShieldCheck, XCircle } from 'lucide-angular';
import {
  IEvaluationResult,
  IRedTeamingDetailedResult,
  IRedTeamingResult,
  IVulnerabilityResult,
} from '../../../model/evaluation-result.model';
import { EvaluationResultsHelperService } from '../evaluation-results-helper.service';

@Component({
  standalone: true,
  selector: 'sm-eval-security-tab',
  imports: [CommonModule, TableModule, TagModule, ProgressBarModule, AccordionModule, TooltipModule, LucideAngularModule],
  styles: [
    `
      ::ng-deep .defense-high .p-progressbar-value {
        background: var(--green-500);
      }
      ::ng-deep .defense-medium .p-progressbar-value {
        background: var(--yellow-500);
      }
      ::ng-deep .defense-low .p-progressbar-value {
        background: var(--red-500);
      }
    `,
  ],
  template: `
    @for (target of result?.targets; track target.name) {
      @if (target.red_teaming && target.red_teaming.vulnerabilities?.length > 0) {
        <div class="mb-4">
          <!-- Summary Header -->
          <div class="flex align-items-center justify-content-between mb-3">
            <div>
              <h4 class="text-sm font-semibold mb-1">Red Team Assessment</h4>
              <span class="text-xs text-500">
                {{ target.red_teaming.vulnerabilities.length }} vulnerabilities · {{ getTotalAttacks(target.red_teaming) }} attacks
              </span>
            </div>
            <div class="flex align-items-center gap-2">
              <span class="text-lg font-bold" [class]="getOverallDefenseClass(target.red_teaming)">
                {{ getOverallDefenseRate(target.red_teaming) | number: '1.0-0' }}%
              </span>
              <span class="text-xs text-500">defense rate</span>
            </div>
          </div>

          <!-- Vulnerability Summary Table -->
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
                      vuln.vulnerability_name || helper.formatVulnerabilityType(vuln.vulnerability_type)
                    }}</span>
                    <div class="text-xs text-500">{{ helper.formatVulnerabilityType(vuln.vulnerability_type) }}</div>
                  </div>
                </td>
                <td class="text-center">{{ vuln.total_attacks }}</td>
                <td class="text-center text-green-500 font-medium">{{ vuln.failed_attacks }}</td>
                <td class="text-center text-red-500 font-medium">{{ vuln.successful_attacks }}</td>
                <td style="width: 150px">
                  <div class="flex align-items-center gap-2">
                    <p-progressBar
                      [value]="helper.getDefenseRate(vuln)"
                      [showValue]="false"
                      [style]="{ width: '80px', height: '6px' }"
                      [styleClass]="getDefenseRateClass(vuln)"
                    ></p-progressBar>
                    <span class="text-xs font-medium">{{ helper.getDefenseRate(vuln) | number: '1.0-0' }}%</span>
                  </div>
                </td>
                <td>
                  <p-tag [severity]="helper.getSeverityTagSeverity(vuln.severity)" [value]="vuln.severity" [rounded]="true"></p-tag>
                </td>
              </tr>
            </ng-template>
          </p-table>

          <!-- Attack Methods Summary -->
          <div class="mt-4 mb-3">
            <h5 class="text-xs font-semibold mb-2 text-500">Attack Methods Used</h5>
            <div class="flex flex-wrap gap-2">
              @for (attack of helper.getAllAttackMethods(target.red_teaming); track attack.method) {
                <p-tag severity="secondary" [rounded]="true">{{ attack.method }}: {{ attack.count }}</p-tag>
              }
            </div>
          </div>

          <!-- Detailed Results Accordion -->
          @if (target.red_teaming.detailed_results && target.red_teaming.detailed_results.length > 0) {
            <p-accordion>
              <p-accordionTab header="Attack Details ({{ target.red_teaming.detailed_results.length }})">
                <!-- Filters -->
                <div class="flex gap-2 mb-3 flex-wrap">
                  <p-tag
                    [severity]="selectedFilter() === 'all' ? 'info' : 'secondary'"
                    value="All"
                    [rounded]="true"
                    class="cursor-pointer"
                    (click)="selectedFilter.set('all')"
                  ></p-tag>
                  <p-tag
                    [severity]="selectedFilter() === 'breached' ? 'danger' : 'secondary'"
                    value="Breached"
                    [rounded]="true"
                    class="cursor-pointer"
                    (click)="selectedFilter.set('breached')"
                  ></p-tag>
                  <p-tag
                    [severity]="selectedFilter() === 'blocked' ? 'success' : 'secondary'"
                    value="Blocked"
                    [rounded]="true"
                    class="cursor-pointer"
                    (click)="selectedFilter.set('blocked')"
                  ></p-tag>
                  <span class="text-300">|</span>
                  @for (vuln of getUniqueVulnerabilities(target.red_teaming); track vuln) {
                    <p-tag
                      [severity]="selectedVuln() === vuln ? 'warning' : 'secondary'"
                      [value]="vuln"
                      [rounded]="true"
                      class="cursor-pointer"
                      (click)="toggleVulnFilter(vuln)"
                    ></p-tag>
                  }
                </div>

                <p-table
                  [value]="filterDetailedResults(target.red_teaming.detailed_results)"
                  styleClass="p-datatable-sm"
                  [paginator]="true"
                  [rows]="10"
                  [rowsPerPageOptions]="[5, 10, 25]"
                >
                  <ng-template pTemplate="header">
                    <tr>
                      <th style="width: 50px">#</th>
                      <th>Vulnerability</th>
                      <th>Attack</th>
                      <th>Input</th>
                      <th>Output</th>
                      <th style="width: 80px">Score</th>
                      <th style="width: 80px">Status</th>
                    </tr>
                  </ng-template>
                  <ng-template pTemplate="body" let-detail>
                    <tr>
                      <td class="text-xs text-500">{{ detail.idx + 1 }}</td>
                      <td>
                        <div class="font-medium text-sm">{{ detail.vulnerability }}</div>
                        <div class="text-xs text-500">{{ helper.formatVulnerabilityType(detail.vulnerability_type) }}</div>
                      </td>
                      <td>
                        <p-tag severity="secondary" [value]="detail.attack_method" [rounded]="true" styleClass="text-xs"></p-tag>
                      </td>
                      <td>
                        <div
                          class="text-sm"
                          style="max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"
                          [pTooltip]="detail.input"
                          tooltipPosition="top"
                          [tooltipOptions]="{ tooltipStyleClass: 'max-w-30rem' }"
                        >
                          {{ truncateText(detail.input, 50) }}
                        </div>
                      </td>
                      <td>
                        <div class="flex align-items-center gap-2">
                          <div
                            class="text-sm"
                            style="max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"
                            [pTooltip]="detail.actual_output"
                            tooltipPosition="top"
                            [tooltipOptions]="{ tooltipStyleClass: 'max-w-30rem' }"
                          >
                            {{ truncateText(detail.actual_output, 50) || '(empty)' }}
                          </div>
                        </div>
                      </td>
                      <td>
                        <span class="font-semibold" [class]="detail.score === 1.0 ? 'text-green-500' : 'text-red-500'">
                          {{ detail.score | number: '1.1-1' }}
                        </span>
                      </td>
                      <td>
                        <div class="flex align-items-center gap-2">
                          @if (detail.success) {
                            <i-lucide [img]="ShieldAlert" class="w-1.25rem h-1.25rem text-red-500" pTooltip="Attack Succeeded"></i-lucide>
                          } @else {
                            <i-lucide [img]="ShieldCheck" class="w-1.25rem h-1.25rem text-green-500" pTooltip="Attack Blocked"></i-lucide>
                          }
                          @if (detail.reason) {
                            <i-lucide
                              [img]="MessageSquare"
                              class="w-1rem h-1rem text-400 cursor-pointer"
                              [pTooltip]="detail.reason"
                              tooltipPosition="left"
                              [tooltipOptions]="{ tooltipStyleClass: 'max-w-25rem' }"
                            ></i-lucide>
                          }
                        </div>
                      </td>
                    </tr>
                  </ng-template>
                  <ng-template pTemplate="emptymessage">
                    <tr>
                      <td colspan="7" class="text-center text-500 py-4">No attacks match the selected filters</td>
                    </tr>
                  </ng-template>
                </p-table>
              </p-accordionTab>
            </p-accordion>
          }
        </div>
      }
    }
  `,
})
export class EvalSecurityTabComponent {
  @Input() result: IEvaluationResult | null = null;

  helper = inject(EvaluationResultsHelperService);

  selectedFilter = signal<'all' | 'breached' | 'blocked'>('all');
  selectedVuln = signal<string | null>(null);

  protected readonly ShieldCheck = ShieldCheck;
  protected readonly ShieldAlert = ShieldAlert;
  protected readonly MessageSquare = MessageSquare;

  getDefenseRateClass(vuln: IVulnerabilityResult): string {
    const rate = this.helper.getDefenseRate(vuln);
    if (rate >= 80) return 'defense-high';
    if (rate >= 50) return 'defense-medium';
    return 'defense-low';
  }

  getTotalAttacks(redTeaming: IRedTeamingResult): number {
    return redTeaming.vulnerabilities.reduce((sum, v) => sum + v.total_attacks, 0);
  }

  getOverallDefenseRate(redTeaming: IRedTeamingResult): number {
    const total = redTeaming.vulnerabilities.reduce((sum, v) => sum + v.total_attacks, 0);
    const blocked = redTeaming.vulnerabilities.reduce((sum, v) => sum + v.failed_attacks, 0);
    return total > 0 ? (blocked / total) * 100 : 0;
  }

  getOverallDefenseClass(redTeaming: IRedTeamingResult): string {
    const rate = this.getOverallDefenseRate(redTeaming);
    if (rate >= 80) return 'text-green-500';
    if (rate >= 50) return 'text-yellow-500';
    return 'text-red-500';
  }

  getUniqueVulnerabilities(redTeaming: IRedTeamingResult): string[] {
    if (!redTeaming.detailed_results) return [];
    return [...new Set(redTeaming.detailed_results.map(d => d.vulnerability))];
  }

  toggleVulnFilter(vuln: string): void {
    this.selectedVuln.set(this.selectedVuln() === vuln ? null : vuln);
  }

  filterDetailedResults(results: IRedTeamingDetailedResult[]): IRedTeamingDetailedResult[] {
    let filtered = results;

    // Filter by status
    if (this.selectedFilter() === 'breached') {
      filtered = filtered.filter(r => r.success);
    } else if (this.selectedFilter() === 'blocked') {
      filtered = filtered.filter(r => !r.success);
    }

    // Filter by vulnerability
    if (this.selectedVuln()) {
      filtered = filtered.filter(r => r.vulnerability === this.selectedVuln());
    }

    return filtered;
  }

  truncateText(text: string, maxLength: number): string {
    if (!text) return '';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  }
}
