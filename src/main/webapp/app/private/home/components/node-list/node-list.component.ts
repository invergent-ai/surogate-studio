import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { Router } from '@angular/router';
import { INode } from '../../../../shared/model/node.model';
import { Account } from '../../../../shared/model/account.model';
import { Subject } from 'rxjs';
import { NgForOf, NgIf } from '@angular/common';
import { NodeCardComponent } from '../../../nodes/components/card/node-card.component';
import { PageLoadComponent } from '../../../../shared/components/page-load/page-load.component';
import { Store } from '@ngxs/store';
import { takeUntil } from 'rxjs/operators';
import { Selectors } from '../../../../shared/state/selectors';
import { PanelModule } from 'primeng/panel';
import { LucideAngularModule, PlusSquare } from 'lucide-angular';

@Component({
  standalone: true,
  selector: 'sm-node-list',
  imports: [
    NodeCardComponent,
    ButtonModule,
    NgIf,
    NgForOf,
    PageLoadComponent,
    PanelModule,
    LucideAngularModule
  ],
  templateUrl: './node-list.component.html'
})
export class NodeListComponent implements OnInit, OnDestroy {
  @Input()
  user!: Account;

  nodelist = true;
  private destroy$ = new Subject<void>();
  nodes: INode[] = [];

  constructor(
    private router: Router,
    private store: Store
  ) {}

  async ngOnInit() {
    this.store.select(Selectors.nodes)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
      next: (nodes) => {
        this.nodes = [...nodes];
        this.nodes.sort((a, b) => a.cluster.zone.zoneId.localeCompare(b.cluster.zone.zoneId));
      }
    });
  }

  async gotoNode(node: INode) {
    await this.router.navigate(['/nodes'], {queryParams: {id: node.id}});
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected readonly PlusSquare = PlusSquare;
}
