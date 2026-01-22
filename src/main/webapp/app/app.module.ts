import {LOCALE_ID, NgModule} from '@angular/core';
import {registerLocaleData} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';
import locale from '@angular/common/locales/en';
import {BrowserModule, provideClientHydration, Title} from '@angular/platform-browser';
import {TitleStrategy} from '@angular/router';

import './config/dayjs';
import {TranslationModule} from 'app/shared/language/translation.module';
import {AppPageTitleStrategy} from './config/app-page-title-strategy';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {NgxsModule} from '@ngxs/store';
import {AppState} from './shared/state/app-state';
import {NgxsReduxDevtoolsPluginModule} from '@ngxs/devtools-plugin';
import {AppRoutingModule} from './app-routing.module';
import {ConfirmationService, MessageService} from 'primeng/api';
import {httpInterceptorProviders} from './shared/interceptor';
import {TrackerService} from './shared/service/tracker.service';
import MainModule from './main/main.module';
import {MainComponent} from './main/main.component';
import {NotificationState} from './shared/state/notification.state';
import {provideHighlightOptions} from 'ngx-highlightjs';

@NgModule({
  imports: [
    NgxsModule.forRoot([AppState, NotificationState], { developmentMode: true }),
    NgxsReduxDevtoolsPluginModule.forRoot(),
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    AppRoutingModule,
    MainModule,
    TranslationModule
  ],
  providers: [
    ConfirmationService,
    MessageService,
    Title,
    { provide: LOCALE_ID, useValue: 'en' },
    httpInterceptorProviders,
    { provide: TitleStrategy, useClass: AppPageTitleStrategy },
    provideClientHydration(),
    provideHighlightOptions({
      fullLibraryLoader: () => import('highlight.js'),
      lineNumbersLoader: () => import('ngx-highlightjs/line-numbers')
    })
  ],
  bootstrap: [MainComponent],
  declarations: []
})

export class AppModule {
  constructor(
    trackerService: TrackerService
  ) {
    trackerService.setup();
    registerLocaleData(locale);
  }
}
