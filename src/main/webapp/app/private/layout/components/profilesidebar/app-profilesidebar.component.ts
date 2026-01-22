import { Component } from '@angular/core';
import { LayoutService } from '../../../../shared/service/theme/app-layout.service';

@Component({
    selector: 'sm-profilemenu',
    templateUrl: './app-profilesidebar.component.html'
})
export class AppProfilesidebarComponent {
    date: Date= new Date();
    constructor(public layoutService: LayoutService) { }

    get visible(): boolean {
        return this.layoutService.state.rightMenuActive;
    }

    set visible(_val: boolean) {
        this.layoutService.state.rightMenuActive = _val;
    }
}
