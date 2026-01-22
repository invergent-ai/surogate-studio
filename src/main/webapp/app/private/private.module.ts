import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { privateRoutes } from './private.route';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { StyleClassModule } from 'primeng/styleclass';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { AsyncPipe, DatePipe, NgClass } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { SidebarModule } from 'primeng/sidebar';
import SharedModule from '../shared/shared.module';
import { AppBreadcrumbComponent } from './layout/components/breadcrumb/app-breadcrumb.component';
import { AppSidebarComponent } from './layout/components/sidebar/app-sidebar.component';
import { AppTopbarComponent } from './layout/components/topbar/app-topbar.component';
import { AppProfilesidebarComponent } from './layout/components/profilesidebar/app-profilesidebar.component';
import { AppMenuComponent } from './layout/components/menu/app-menu.component';
import { AppMenuitemComponent } from './layout/components/menu-item/app-menuitem.component';
import { PrivateLayoutComponent } from './layout/components/layout/private-layout.component';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { NotificationListComponent } from '../shared/components/notifications/notification-list.component';
import { NotificationDialogComponent } from '../shared/components/notifications/notification-dialog.component';
import { LucideAngularModule } from 'lucide-angular';
import { HelpComponent } from '../shared/components/help/help.component';

@NgModule({
  imports: [
    RouterModule.forChild(privateRoutes),
    SharedModule,
    FormsModule,
    StyleClassModule,
    ReactiveFormsModule,
    InputTextModule,
    ButtonModule,
    NgClass,
    TooltipModule,
    AsyncPipe,
    SidebarModule,
    DatePipe,
    OverlayPanelModule,
    NotificationListComponent,
    NotificationDialogComponent,
    LucideAngularModule,
    HelpComponent
  ],
  declarations: [
    AppBreadcrumbComponent,
    AppSidebarComponent,
    AppTopbarComponent,
    AppProfilesidebarComponent,
    AppMenuComponent,
    AppMenuitemComponent,
    PrivateLayoutComponent
  ],
  providers: [],
  exports: [
    AppBreadcrumbComponent
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class PrivateModule {

}
