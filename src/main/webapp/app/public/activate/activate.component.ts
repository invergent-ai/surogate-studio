import {Component, Inject, OnInit, PLATFORM_ID} from '@angular/core';
import {CommonModule, isPlatformBrowser} from '@angular/common';
import { RouterModule } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { mergeMap } from 'rxjs/operators';

// PrimeNG Imports
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { MessagesModule } from 'primeng/messages';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import {MessageService, SharedModule} from 'primeng/api';

// Services and Shared
import { ActivateService } from '../../shared/service/activate.service';
import { LayoutService } from '../../shared/service/theme/app-layout.service';

@Component({
  selector: 'sm-activate',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    RouterModule,
    ButtonModule,
    CardModule,
    MessageModule,
    MessagesModule,
    ProgressSpinnerModule
  ],
  providers: [MessageService, LayoutService],
  templateUrl: './activate.component.html',
})
export default class ActivateComponent implements OnInit {
  error = false;
  success = false;
  loading = true;
  isBrowser = false;

  constructor(
    private activateService: ActivateService,
    private route: ActivatedRoute,
    private messageService: MessageService,
    public layoutService: LayoutService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  get dark(): boolean {
    return this.layoutService.config.colorScheme !== 'light';
  }

  ngOnInit(): void {
    // Clear any existing messages
    this.messageService.clear();

    // Process activation
    this.route.queryParams.pipe(
      mergeMap(params => {
        if (!this.isBrowser) {
          return new Promise(null);
        }
        const key = params['key'];
        if (!key) {
          throw new Error('Activation key not found');
        }
        return this.activateService.get(key);
      })
    ).subscribe({
      next: () => {
        this.success = true;
        this.loading = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Your account has been activated successfully.'
        });
      },
      error: (error) => {
        this.error = true;
        this.loading = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Account activation failed. Please try registering again.'
        });
        console.error('Activation error:', error);
      },
    });
  }
}
