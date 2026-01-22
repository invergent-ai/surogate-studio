import {Component, OnInit, Renderer2, RendererFactory2} from '@angular/core';
import {LangChangeEvent, TranslateService} from '@ngx-translate/core';
import dayjs from 'dayjs/esm';

import {AppPageTitleStrategy} from 'app/config/app-page-title-strategy';
import {Router} from '@angular/router';
import { MessageService, PrimeNGConfig } from 'primeng/api';
import { Actions, ofActionDispatched, Store } from '@ngxs/store';
import {Observable} from "rxjs";
import {Selectors} from '../shared/state/selectors';
import {AccountService} from '../shared/service/account.service';
import {LayoutService} from '../shared/service/theme/app-layout.service';
import { DisplayGlobalMessageAction, ReportGlobalErrorAction } from '../shared/state/actions';

@Component({
  selector: 'sm-main',
  templateUrl: './main.component.html',
  providers: [AppPageTitleStrategy],
})
export class MainComponent implements OnInit {
  public globalError$: Observable<string>;
  public hasGlobalError$: Observable<boolean>;
  private renderer: Renderer2;

  constructor(
    private router: Router,
    private appPageTitleStrategy: AppPageTitleStrategy,
    private accountService: AccountService,
    private translateService: TranslateService,
    private primengConfig: PrimeNGConfig,
    private layoutService: LayoutService,
    private messageService: MessageService,
    private store: Store,
    private actions: Actions,
    rootRenderer: RendererFactory2,
  ) {
    this.renderer = rootRenderer.createRenderer(document.querySelector('html'), null);
    this.globalError$ = this.store.select((state: any) => state.app.globalError);
    this.hasGlobalError$ = this.store.select(Selectors.hasGlobalError);
  }

  ngOnInit(): void {
    // try to log in automatically
    this.accountService.identity(true, false).subscribe();

    this.translateService.onLangChange.subscribe((langChangeEvent: LangChangeEvent) => {
      this.appPageTitleStrategy.updateTitle(this.router.routerState.snapshot);
      dayjs.locale(langChangeEvent.lang);
      this.renderer.setAttribute(document.querySelector('html'), 'lang', langChangeEvent.lang);
    });

    this.actions.pipe(
      ofActionDispatched(DisplayGlobalMessageAction)
    ).subscribe(action => {
      this.messageService.add(action.message);
    });

    this.primengConfig.ripple = true;
    this.layoutService.applyScale();
  }

  dismissError() {
    this.store.dispatch(new ReportGlobalErrorAction(null));
  }
}
