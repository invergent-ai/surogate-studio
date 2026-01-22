import {Component} from '@angular/core';
import { LayoutService } from '../../shared/service/theme/app-layout.service';

@Component({
    templateUrl: './accessdenied.component.html'
})
export class AccessdeniedComponent {
    constructor(public layoutService: LayoutService) {

    }
}
