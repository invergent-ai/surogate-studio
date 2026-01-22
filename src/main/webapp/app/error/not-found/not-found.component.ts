import {Component} from "@angular/core";
import { LayoutService } from '../../shared/service/theme/app-layout.service';

@Component({
  selector: 'sm-not-found',
  templateUrl: './not-found.component.html',
})
export class NotFoundComponent {
    constructor(public layoutService: LayoutService) {
    }
}
