import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Data, ParamMap, Router, RouterModule} from '@angular/router';
import {combineLatest, Observable, switchMap, tap} from 'rxjs';

import SharedModule from 'app/shared/shared.module';
import {FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ASC, DEFAULT_SORT_DATA, DESC, SORT} from 'app/config/constant/navigation.constants';
import {SortService} from 'app/shared/sort/sort.service';
import {HttpResponse} from "@angular/common/http";
import {IZone} from "../../../../shared/model/zone.model";
import {ZoneService} from "../../../../shared/service/zone.service";
import {ClusterFormService} from "../../../../shared/service/form/cluster-form.service";
import {ConfirmationService, MessageService} from "primeng/api";
import {finalize} from "rxjs/operators";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ToastModule} from "primeng/toast";
import {DropdownModule} from "primeng/dropdown";
import {InputTextareaModule} from "primeng/inputtextarea";
import {InputTextModule} from "primeng/inputtext";
import {DialogModule} from "primeng/dialog";
import {ButtonModule} from "primeng/button";
import {TableModule} from "primeng/table";
import {TooltipModule} from "primeng/tooltip";
import {ICluster} from '../../../../shared/model/cluster.model';
import {ClusterService, EntityArrayResponseType} from '../../../../shared/service/cluster.service';
import {LayoutService} from "../../../../shared/service/theme/app-layout.service";

@Component({
  standalone: true,
  selector: 'sm-cluster',
  templateUrl: './cluster.component.html',
  imports: [
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    ConfirmDialogModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    InputTextareaModule,
    DropdownModule,
    ToastModule,
    TooltipModule,
  ],
})
export class ClusterComponent implements OnInit {
  clusters: ICluster[] = [];
  displayDialog = false;
  isNewCluster = true;
  isLoading = false;
  clusterForm: FormGroup;
  zones: IZone[] = [];
  predicate = 'id';
  ascending = true;

  constructor(
    public layoutService: LayoutService,
    protected clusterService: ClusterService,
    protected activatedRoute: ActivatedRoute,
    public router: Router,
    protected sortService: SortService,
    private zoneService: ZoneService,
    private clusterFormService: ClusterFormService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {
    this.clusterForm = this.clusterFormService.createClusterFormGroup();
  }

  ngOnInit(): void {
    this.loadClusters();
    this.loadZones();
  }

  loadClusters(): void {
    this.isLoading = true;
    this.clusterService.query().subscribe({
      next: (res: HttpResponse<ICluster[]>) => {
        this.clusters = res.body ?? [];
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  loadZones(): void {
    this.zoneService.query().subscribe(
      (res: HttpResponse<IZone[]>) => {
        this.zones = res.body ?? [];
      }
    );
  }

  showDialog(cluster?: ICluster): void {
    this.isNewCluster = !cluster;
    this.clusterForm.reset();

    if (cluster) {
      this.clusterForm.patchValue(cluster);
    }

    this.displayDialog = true;
  }

  hideDialog(): void {
    this.displayDialog = false;
  }

  save(): void {
    if (this.clusterForm.valid) {
      const cluster = this.clusterFormService.getCluster(this.clusterForm);

      if (this.isNewCluster) {
        this.subscribeToSaveResponse(this.clusterService.create(cluster));
      } else {
        const existing = this.clusters.filter(c => c.id === cluster.id)[0];
        cluster.redisUrl = existing.redisUrl;
        cluster.prometheusUrl = existing.prometheusUrl;
        this.subscribeToSaveResponse(this.clusterService.update(cluster));
      }
    }
  }

  confirmDelete(cluster: ICluster): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete Cluster ${cluster.name}?`,
      header: 'Confirm Delete Operation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.clusterService.delete(cluster.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Successful',
              detail: 'Cluster Deleted',
              life: 3000
            });
            this.loadClusters();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: 'Failed to delete cluster',
              life: 3000
            });
          }
        });
      }
    });
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ICluster>>): void {
    result.pipe(finalize(() => this.hideDialog())).subscribe({
      next: () => {
        this.loadClusters();
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `Cluster ${this.isNewCluster ? 'created' : 'updated'} successfully`
        });
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: `Failed to ${this.isNewCluster ? 'create' : 'update'} cluster`
        });
      }
    });
  }

  load(): void {
    this.loadFromBackendWithRouteInformations().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
      },
    });
  }

  protected loadFromBackendWithRouteInformations(): Observable<EntityArrayResponseType> {
    return combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data]).pipe(
      tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
      switchMap(() => this.queryBackend(this.predicate, this.ascending)),
    );
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
    this.clusters = this.refineData(dataFromBody);
  }

  protected refineData(data: ICluster[]): ICluster[] {
    return data.sort(this.sortService.startSort(this.predicate, this.ascending ? 1 : -1));
  }

  protected fillComponentAttributesFromResponseBody(data: ICluster[] | null): ICluster[] {
    return data ?? [];
  }

  protected queryBackend(predicate?: string, ascending?: boolean): Observable<EntityArrayResponseType> {
    this.isLoading = true;
    const queryObject: any = {
      sort: this.getSortQueryParam(predicate, ascending),
    };
    return this.clusterService.query(queryObject).pipe(tap(() => (this.isLoading = false)));
  }

  protected handleNavigation(predicate?: string, ascending?: boolean): void {
    const queryParamsObj = {
      sort: this.getSortQueryParam(predicate, ascending),
    };

    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams: queryParamsObj,
    });
  }

  protected getSortQueryParam(predicate = this.predicate, ascending = this.ascending): string[] {
    const ascendingQueryParam = ascending ? ASC : DESC;
    if (predicate === '') {
      return [];
    } else {
      return [predicate + ',' + ascendingQueryParam];
    }
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const sort = (params.get(SORT) ?? data[DEFAULT_SORT_DATA]).split(',');
    this.predicate = sort[0];
    this.ascending = sort[1] === ASC;
  }

  onSort(event: any): void {
    const predicate = event.field;
    const ascending = event.order === 1;
    if (predicate !== this.predicate || ascending !== this.ascending) {
      this.predicate = predicate;
      this.ascending = ascending;
      this.navigateToWithComponentValues();
    }
  }

  navigateToWithComponentValues(): void {
    this.handleNavigation(this.predicate, this.ascending);
  }
}
