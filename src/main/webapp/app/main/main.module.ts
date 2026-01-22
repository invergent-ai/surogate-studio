import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import {MainComponent} from './main.component';
import {AsyncPipe} from "@angular/common";
import {NotFoundComponent} from "../error/not-found/not-found.component";
import {AccessdeniedComponent} from "../error/accessdenied/accessdenied.component";

@NgModule({
  imports: [
    SharedModule,
    RouterModule,
    AsyncPipe
  ],
  declarations: [
    MainComponent,
    NotFoundComponent,
    AccessdeniedComponent
  ],
})
export default class MainModule {}
