import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Data, ParamMap, Router, RouterModule} from '@angular/router';
import {combineLatest, Observable, switchMap, tap} from 'rxjs';

import SharedModule from 'app/shared/shared.module';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ASC, DEFAULT_SORT_DATA, DESC, SORT} from 'app/config/constant/navigation.constants';
import {SortService} from 'app/shared/sort/sort.service';
import {ISystemConfiguration} from '../../../../shared/model/system-configuration.model';
import {
  EntityArrayResponseType,
  SystemConfigurationService
} from '../../../../shared/service/system-configuration.service';
import {MessageService} from "primeng/api";
import {SystemConfigurationFormService} from "../../../../shared/service/form/system-configuration-form.service";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {TooltipModule} from "primeng/tooltip";
import {TableModule} from "primeng/table";
import {DialogModule} from "primeng/dialog";
import {PaginatorModule} from "primeng/paginator";
import {CheckboxModule} from "primeng/checkbox";
import {ToastModule} from "primeng/toast";
import { CardModule } from 'primeng/card';
import { PageComponent } from '../../../../shared/components/page/page.component';
import { PageLoadComponent } from '../../../../shared/components/page-load/page-load.component';

@Component({
  standalone: true,
  selector: 'sm-system-configuration',
  templateUrl: './system-configuration.component.html',
  imports: [
    RouterModule,
    FormsModule,
    SharedModule,
    ConfirmDialogModule,
    ReactiveFormsModule,
    TooltipModule,
    TableModule,
    DialogModule,
    PaginatorModule,
    CheckboxModule,
    ToastModule,
    CardModule,
    PageComponent,
    PageLoadComponent
  ]
})
export class SystemConfigurationComponent implements OnInit {
  systemConfigurations?: ISystemConfiguration[];
  isLoading = false;
  isSaving = false;
  dialogVisible = false;
  selectedConfig: ISystemConfiguration = {} as ISystemConfiguration;
  editForm: any;

  predicate = 'id';
  ascending = true;

  constructor(
    protected systemConfigurationService: SystemConfigurationService,
    protected systemConfigurationFormService: SystemConfigurationFormService,
    protected activatedRoute: ActivatedRoute,
    public router: Router,
    public sortService: SortService,
    private messageService: MessageService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  save(): void {
    this.isSaving = true;
    const config = this.systemConfigurationFormService.getSystemConfiguration(this.editForm);

    if (config.id) {
      this.systemConfigurationService.update(config).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: 'System Configuration Updated',
            life: 3000
          });
          this.load();
          this.hideDialog();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to update configuration',
            life: 3000
          });
        },
        complete: () => {
          this.isSaving = false;
        }
      });
    }
  }

  load(): void {
    this.loadFromBackendWithRouteInformations().subscribe({
      next: (res: any) => {
        this.onResponseSuccess(res);
      },
    });
  }

  openDialog(config?: ISystemConfiguration): void {
    this.selectedConfig = config ? {...config} : {} as ISystemConfiguration;
    this.dialogVisible = true;
    if (config) {
      this.editForm = this.systemConfigurationFormService.createSystemConfigurationFormGroup(config);
    } else {
      this.editForm = this.systemConfigurationFormService.createSystemConfigurationFormGroup();
    }
  }

  hideDialog(): void {
    this.dialogVisible = false;
    this.selectedConfig = {} as ISystemConfiguration;
  }

  protected loadFromBackendWithRouteInformations(): Observable<EntityArrayResponseType> {
    return combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data]).pipe(
      tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
      switchMap(() => this.queryBackend(this.predicate, this.ascending)),
    );
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const sort = (params.get(SORT) ?? data[DEFAULT_SORT_DATA]).split(',');
    this.predicate = sort[0];
    this.ascending = sort[1] === ASC;
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
    this.systemConfigurations = this.refineData(dataFromBody);
  }

  protected refineData(data: ISystemConfiguration[]): ISystemConfiguration[] {
    return data.sort(this.sortService.startSort(this.predicate, this.ascending ? 1 : -1));
  }

  protected fillComponentAttributesFromResponseBody(data: ISystemConfiguration[] | null): ISystemConfiguration[] {
    return data ?? [];
  }

  protected queryBackend(predicate?: string, ascending?: boolean): Observable<EntityArrayResponseType> {
    this.isLoading = true;
    const queryObject: any = {
      sort: this.getSortQueryParam(predicate, ascending),
    };
    return this.systemConfigurationService.query(queryObject).pipe(tap(() => (this.isLoading = false)));
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
