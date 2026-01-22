import {Component, computed} from "@angular/core";
import {PageComponent} from "../../../shared/components/page/page.component";
import {PageLoadComponent} from "../../../shared/components/page-load/page-load.component";
import {InputTextModule} from "primeng/inputtext";
import {DropdownModule} from "primeng/dropdown";
import {LucideAngularModule, Plus} from "lucide-angular";
import {TableModule} from "primeng/table";
import {TagModule} from "primeng/tag";
import {OverlayPanelModule} from "primeng/overlaypanel";
import {ProgressBarModule} from "primeng/progressbar";
import {injectParams} from "ngxtension/inject-params";
import {CardComponent} from "../../../shared/components/card/card.component";
import SharedModule from "../../../shared/shared.module";
import {RouterLink} from "@angular/router";

export type TaskType = 'factual' | 'synthetic' | 'test';

@Component({
  standalone: true,
  selector: 'sm-data-tasks-page',
  templateUrl: './data-tasks.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    InputTextModule,
    DropdownModule,
    LucideAngularModule,
    TableModule,
    TagModule,
    OverlayPanelModule,
    ProgressBarModule,
    CardComponent,
    SharedModule,
    RouterLink
  ]
})
export class DataTasksPage {
  type = injectParams('type');
  pageTitle = computed(() => {
    switch (this.type() as TaskType) {
      case 'factual':
        return 'Factual Data';
      case 'synthetic':
        return 'Synthetic Data';
      case 'test':
        return 'Test Data';
      default:
        return '';
    }
  });
  protected readonly Plus = Plus;
}
