import { Component, effect, EventEmitter, input, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { INode } from '../../../../shared/model/node.model';
import { NgxFilesizeModule } from 'ngx-filesize';
import SharedModule from '../../../../shared/shared.module';
import { TagModule } from 'primeng/tag';
import { InplaceModule } from 'primeng/inplace';
import { NodeStatusService } from '../../../../shared/service/k8s/node-status.service';
import { lastValueFrom } from 'rxjs';
import { NodeStats } from '../../../../shared/model/k8s/node-stats.model';
import { AccountService } from '../../../../shared/service/account.service';
import { Account } from '../../../../shared/model/account.model';
import { NodeStatus } from '../../../../shared/model/enum/node-status.model';
import DurationPipe from '../../../../shared/date/duration.pipe';
import { ApplicationStatus } from '../../../../shared/model/enum/application-status.model';
import { Store } from '@ngxs/store';
import { LoadNodesAction } from '../../../../shared/state/actions';
import { GpuCardsComponent } from '../gpu-cards/gpu-cards.component';
import { displayError } from '../../../../shared/util/error.util';
import {
  Activity,
  ArrowDown,
  ArrowUp,
  Clock,
  Cpu,
  HardDrive,
  Info,
  Layers,
  LucideAngularModule,
  Server,
  Shield,
  TriangleAlert
} from 'lucide-angular';
import { CardModule } from 'primeng/card';

@Component({
  selector: 'sm-node-card',
  templateUrl: './node-card.component.html',
  styleUrls: ['./node-card.component.scss'],
  standalone: true,
  imports: [
    NgxFilesizeModule,
    SharedModule,
    TagModule,
    InplaceModule,
    DurationPipe,
    GpuCardsComponent,
    LucideAngularModule,
    CardModule
  ]
})
export class NodeCardComponent implements OnInit, OnDestroy {
  node = input.required<INode>();

  @Input() nodelist = false;
  @Input() editable = false;
  @Input() showGpus = false;
  @Output() onNameChanged = new EventEmitter<string>();
  @Output() onGpuClicked = new EventEmitter<number>();
  @Output() onNodeStats = new EventEmitter<NodeStats>();

  nodeName: string;
  status: NodeStats;
  statusLoading = true;
  user: Account;

  constructor(private nodeStatusService: NodeStatusService,
              private accountService: AccountService,
              private store: Store) {
    effect(async () => {
      if (this.node()?.id) {
        await this.initStatus();
        this.nodeName = this.node()?.name || '';
      }
    });
  }

  async ngOnInit() {
    this.user = await lastValueFrom(this.accountService.identity());
  }

  async initStatus() {
    if (this.node().id) {
      this.statusLoading = true;
      try {
        this.status = await lastValueFrom(this.nodeStatusService.statusSnapshotForNode(this.node().id));
        this.onNodeStats.emit(this.status);
      } catch (e) {
        displayError(this.store, e);
      } finally {
        this.statusLoading = false;
      }
    }
  }

  datacenterSelected(event: Event, datacenterName: string) {
    event.stopPropagation();
    this.store.dispatch(new LoadNodesAction(datacenterName));
  }

  updateName() {
    this.onNameChanged.emit(this.nodeName);
  }

  async destroyStatus() {
    if (this.node()?.id) {
      await lastValueFrom(this.nodeStatusService.stopStatus(this.node()?.id));
    }
  }

  async ngOnDestroy() {
    await this.destroyStatus();
  }

  protected readonly ApplicationStatus = ApplicationStatus;
  protected readonly Server = Server;
  protected readonly Cpu = Cpu;
  readonly NodeStatus = NodeStatus;
  protected readonly HardDrive = HardDrive;
  protected readonly Activity = Activity;
  protected readonly Layers = Layers;
  protected readonly ArrowUp = ArrowUp;
  protected readonly ArrowDown = ArrowDown;
  protected readonly Clock = Clock;
  protected readonly Shield = Shield;
  protected readonly Info = Info;
  protected readonly TriangleAlert = TriangleAlert;
}
