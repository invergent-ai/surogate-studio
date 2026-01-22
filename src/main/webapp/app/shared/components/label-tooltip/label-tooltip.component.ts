import { Component, Input } from '@angular/core';
import { TooltipModule } from 'primeng/tooltip';
import { Info, LucideAngularModule } from 'lucide-angular';

@Component({
  standalone: true,
  selector: 'sm-label-tooltip',
  imports: [
    TooltipModule,
    LucideAngularModule
  ],
  template: `
    <label class="inline-flex gap-1 align-items-baseline">
      <ng-content></ng-content>
      <i-lucide [img]="Info" class="w-1rem h-1rem" [escape]="false" [pTooltip]="tooltipContent"></i-lucide>
    </label>
    <ng-template #tooltipContent>
      <div class="text-sm" [innerHtml]="tooltip"></div>
    </ng-template>
  `,
  styles: [`
    ::ng-deep .p-tooltip {
      max-width: 40em !important;

      .p-tooltip-text {
        padding: 1rem !important;
        white-space: normal;

        p {
          margin-bottom: 1rem;
        }

        ul {
          li {
            margin-bottom: 1rem;
          }
        }

        * {
          &:last-child {
            margin-bottom: 0;
          }
        }
      }
    }
  `]
})
export class LabelTooltipComponent {
  @Input()
  tooltip: string;
  protected readonly Info = Info;
}
