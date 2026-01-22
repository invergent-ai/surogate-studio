import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Data, ParamMap, Router, RouterModule} from '@angular/router';
import {combineLatest, Observable, switchMap, tap} from 'rxjs';

import SharedModule from 'app/shared/shared.module';
import {FormsModule} from '@angular/forms';
import {ASC, DEFAULT_SORT_DATA, DESC, SORT} from 'app/config/constant/navigation.constants';
import {SortService} from 'app/shared/sort/sort.service';
import {IZone} from '../../../../shared/model/zone.model';
import {EntityArrayResponseType, ZoneService} from '../../../../shared/service/zone.service';
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ConfirmationService, MessageService} from "primeng/api";
import {TooltipModule} from "primeng/tooltip";
import {OrganizationService} from '../../../../shared/service/organization.service';
import { LayoutService } from '../../../../shared/service/theme/app-layout.service';

@Component({
  standalone: true,
  selector: 'sm-zone',
  templateUrl: './zone.component.html',
  imports: [
    RouterModule,
    FormsModule,
    SharedModule,
    ConfirmDialogModule,
    TooltipModule
  ],
})
export class ZoneComponent implements OnInit {
  zones?: IZone[];
  isLoading = false;
  isSaving = false;
  dialogVisible = false;
  predicate = 'id';
  ascending = true;
  selectedZone: IZone = {id: undefined} as IZone;
  organizations: any[] = [];
  isLoadingOrganizations = false;

  constructor(
    public layoutService: LayoutService,
    protected zoneService: ZoneService,
    protected activatedRoute: ActivatedRoute,
    public router: Router,
    protected sortService: SortService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    protected organizationService: OrganizationService,
  ) {
  }

  ngOnInit(): void {
    this.loadOrganizations();
    this.load();

  }

  openDialog(zone?: IZone): void {
    this.selectedZone = zone ? {...zone} : {
      id: undefined,
      name: undefined,
      zoneId: undefined,
      vpnApiKey: undefined,
      iperfIp: undefined,
      organization: undefined
    };
    this.dialogVisible = true;
  }

  hideDialog(): void {
    this.dialogVisible = false;
    this.selectedZone = {
      id: undefined,
      name: undefined,
      zoneId: undefined,
      vpnApiKey: undefined,
      iperfIp: undefined,
      organization: undefined
    };
  }

  load(): void {
    this.loadFromBackendWithRouteInformations().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
      },
    });
  }

  saveZone(): void {
    this.isSaving = true;
    const zoneToSave = {...this.selectedZone};
    if (zoneToSave.id) {
      this.zoneService.update(zoneToSave).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Successful',
            detail: 'Zone Updated',
            life: 3000
          });
          this.load();
          this.hideDialog();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to update zone',
            life: 3000
          });
        },
        complete: () => {
          this.isSaving = false;
        }
      });
    } else {
      this.zoneService.create(zoneToSave).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Successful',
            detail: 'Zone Created',
            life: 3000
          });
          this.load();
          this.hideDialog();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to create zone',
            life: 3000
          });
        },
        complete: () => {
          this.isSaving = false;
        }
      });
    }
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

  deleteZone(zone: IZone): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete Zone ${zone.name}?`,
      header: 'Confirm Delete Operation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.zoneService.delete(zone.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Successful',
              detail: 'Zone Deleted',
              life: 3000
            });
            this.load();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Error',
              detail: 'Failed to delete zone',
              life: 3000
            });
          }
        });
      }
    });
  }

  navigateToWithComponentValues(): void {
    this.handleNavigation(this.predicate, this.ascending);
  }

  protected loadFromBackendWithRouteInformations(): Observable<EntityArrayResponseType> {
    return combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data]).pipe(
      tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
      switchMap(() => this.queryBackend(this.predicate, this.ascending)),
    );
  }

  loadOrganizations(): void {
    this.isLoadingOrganizations = true;
    this.organizationService.query().subscribe({
      next: (res) => {
        // Make sure to create a new array reference
        this.organizations = [...(res.body ?? [])];
        this.isLoadingOrganizations = false;
      },
      error: (error) => {
        console.error('Error loading organizations:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load organizations',
          life: 3000
        });
        this.isLoadingOrganizations = false;
      }
    });
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const sort = (params.get(SORT) ?? data[DEFAULT_SORT_DATA]).split(',');
    this.predicate = sort[0];
    this.ascending = sort[1] === ASC;
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
    this.zones = [...this.refineData(dataFromBody)];
  }

  protected refineData(data: IZone[]): IZone[] {
    return data.sort(this.sortService.startSort(this.predicate, this.ascending ? 1 : -1));
  }

  protected fillComponentAttributesFromResponseBody(data: IZone[] | null): IZone[] {
    return data ?? [];
  }

  protected queryBackend(predicate?: string, ascending?: boolean): Observable<EntityArrayResponseType> {
    this.isLoading = true;
    const queryObject: any = {
      sort: this.getSortQueryParam(predicate, ascending),
    };
    return this.zoneService.query(queryObject).pipe(tap(() => (this.isLoading = false)));
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
}
