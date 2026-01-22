import { Component, OnDestroy, OnInit } from '@angular/core';
import { PanelModule } from 'primeng/panel';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { INode } from '../../shared/model/node.model';
import { NgIf } from '@angular/common';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { InplaceModule } from 'primeng/inplace';
import SharedModule from '../../shared/shared.module';
import { NgxFilesizeModule } from 'ngx-filesize';
import { TagModule } from 'primeng/tag';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { NodeCardComponent } from './components/card/node-card.component';
import { PageLoadComponent } from '../../shared/components/page-load/page-load.component';
import { displayError } from '../../shared/util/error.util';
import { PageComponent } from '../../shared/components/page/page.component';
import { Store } from '@ngxs/store';
import { takeUntil } from 'rxjs/operators';
import { Selectors } from '../../shared/state/selectors';
import { LayoutService } from '../../shared/service/theme/app-layout.service';
import { ChartModule } from 'primeng/chart';
import { GpuStatsComponent } from './components/gpu-stats/gpu-stats.component';
import { NodeStats } from '../../shared/model/k8s/node-stats.model';

@Component({
  standalone: true,
  templateUrl: './node.page.html',
  imports: [
    PanelModule,
    NgIf,
    ProgressSpinnerModule,
    InplaceModule,
    SharedModule,
    NgxFilesizeModule,
    TagModule,
    NodeCardComponent,
    ReactiveFormsModule,
    SelectButtonModule,
    ConfirmPopupModule,
    PageLoadComponent,
    PageComponent,
    ChartModule,
    GpuStatsComponent
  ],
  styles: [`
    .node-name .p-inplace-display {
      padding: 0 !important;
    }
  `
  ]
})
export class NodePage implements OnInit, OnDestroy {
  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' }
  ];

  destroy$ = new Subject<void>();
  loading = true;
  lockScreen = false;

  node: INode;
  nodeNameForm: FormGroup;

  gpuId: number = -1;

  constructor(
    public layoutService: LayoutService,
    private route: ActivatedRoute,
    private store: Store) {
  }

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
    this.route.queryParams.subscribe(async params => {
      try {
        const id = params['id'];
        this.store.select(Selectors.nodes)
          .pipe(
            debounceTime(150),
            distinctUntilChanged(),
            takeUntil(this.destroy$)
          )
          .subscribe({
            next: async (nodes) => {
              if (nodes?.length) {
                const currentNode = nodes.find(node => node.id === id);
                if (currentNode) {
                  this.node = JSON.parse(JSON.stringify(currentNode));
                  this.patchForms(currentNode);
                }
              }
            },
            error: (_) => displayError(this.store, new Error('Failed to update node data'))
          });
      } catch (e) {
        displayError(this.store, e);
      } finally {
        this.loading = false;
      }
    });
  }

  private patchForms(currentNode: INode) {
    this.nodeNameForm?.patchValue({
      nodeName: currentNode.name
    }, { emitEvent: false });
  }

  selectGpu(gpuId: number) {
    this.gpuId = gpuId;
  }

  setDefaultGpu(stats: NodeStats) {
    if (stats?.gpuCount > 0) {
      this.selectGpu(0);
    }
  }

  async ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
