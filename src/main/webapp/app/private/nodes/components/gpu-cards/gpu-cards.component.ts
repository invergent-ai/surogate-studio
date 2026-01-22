import {
  ChangeDetectionStrategy,
  Component,
  computed,
  EventEmitter,
  input,
  Input,
  Output,
  Signal
} from '@angular/core';
import { NodeStats } from '../../../../shared/model/k8s/node-stats.model';
import { GpuCard } from '../../../../shared/model/gpu.model';
import { TooltipModule } from 'primeng/tooltip';
import { roundUpTo } from '../../../../shared/util/display.util';
import { Gpu, Info, LucideAngularModule, Server } from 'lucide-angular';
import { CardModule } from 'primeng/card';
import SharedModule from '../../../../shared/shared.module';
import { LabelTooltipComponent } from '../../../../shared/components/label-tooltip/label-tooltip.component';

@Component({
  standalone: true,
  selector: 'sm-gpu-cards',
  templateUrl: './gpu-cards.component.html',
  imports: [
    TooltipModule,
    LucideAngularModule,
    CardModule,
    SharedModule,
    LabelTooltipComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GpuCardsComponent {
  nodeStats= input.required<NodeStats>();
  @Input() clickable = true;
  @Input() selectable = false;
  @Input() multiselect = false;
  @Input() compact = true;
  @Output() onGpuClicked = new EventEmitter<number>();
  @Output() onGpuSelected = new EventEmitter<GpuCard[]>();

  gpuCards: Signal<GpuCard[]>;
  selectedGpuCards: number[] = [];

  constructor() {
    this.gpuCards = computed(() => {
      const status = this.nodeStats();
      if (status && status.gpuCount) {
        const gpus = Array.from({ length: status.gpuCount }, (_, index) => ({
          id: index,
          model: status.gpuModel || 'Unknown GPU',
          gpuMemory: Math.ceil(status.gpuMemory[index] / 1024) || 0,
          gpuMemoryUsage: status.gpuMemoryUsage[index] || 0,
        }));
        if (gpus.length > 0) {
          this.selectedGpuCards = [gpus[0].id];
          this.onGpuSelected.emit([gpus[0]]);
        }
        return gpus;
      }

      return [];
    });
  }

  memBarHeight(gpu: GpuCard): number {
    return roundUpTo(Math.max(0, Math.min(100, gpu.gpuMemoryUsage / (gpu.gpuMemory * 1024) * 100)), 0);
  }

  selectGpu(gpu: number) {
    if (!this.clickable) {
      return;
    }
    this.onGpuClicked.emit(gpu);
  }

  toFriendlyModel(model: string): string {
    switch (model) {
      case 'rtx5090': return 'NVIDIA RTX 5090';
      case 'rtx4070ti': return 'NVIDIA RTX 4070';
      case 'rtx6000pro': return 'NVIDIA RTX Pro 6000';
      default: return 'unknown';
    }
  }

  emitGpuSelection() {
    this.onGpuSelected.emit(
      this.selectedGpuCards.map(
        gpuId => this.gpuCards().find(gpu => gpu.id === gpuId)
      )
    );
  }

  protected readonly roundUpTo = roundUpTo;
  protected readonly Server = Server;
  protected readonly Gpu = Gpu;
  protected readonly Info = Info;
}
