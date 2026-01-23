// src/app/shared/components/evaluation-results/components/eval-security-tab.component.ts
import { Component, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ProgressBarModule } from 'primeng/progressbar';
import { IEvaluationResult, IVulnerabilityResult } from '../../../model/evaluation-result.model';
import { EvaluationResultsHelperService } from '../evaluation-results-helper.service';

@Component({
  standalone: true,
  selector: 'sm-eval-security-tab',
  imports: [CommonModule, TableModule, TagModule, ProgressBarModule],
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

          <div class="mt-4">
            <h5 class="text-xs font-semibold mb-2 text-500">Attack Methods Used</h5>
            <div class="flex flex-wrap gap-2">
              @for (attack of helper.getAllAttackMethods(target.red_teaming); track attack.method) {
                <p-tag severity="secondary" [rounded]="true">{{ attack.method }}: {{ attack.count }}</p-tag>
              }
            </div>
          </div>
        </div>
      }
    }
  `,
})
export class EvalSecurityTabComponent {
  @Input() result: IEvaluationResult | null = null;

  helper = inject(EvaluationResultsHelperService);

  getDefenseRateClass(vuln: IVulnerabilityResult): string {
    const rate = this.helper.getDefenseRate(vuln);
    if (rate >= 80) return 'defense-high';
    if (rate >= 50) return 'defense-medium';
    return 'defense-low';
  }
}
