import { Component, OnDestroy, OnInit, viewChild } from '@angular/core';
import { AccountService } from '../../../../shared/service/account.service';
import { lastValueFrom, Subject, Subscription } from 'rxjs';
import { INode } from '../../../../shared/model/node.model';
import { IApplication } from '../../../../shared/model/application.model';
import { Store } from '@ngxs/store';
import { IUpdateMessage, UpdateMessageAction } from '../../../../shared/state/actions';
import { TrackerService } from '../../../../shared/service/tracker.service';
import { map, takeUntil } from 'rxjs/operators';
import { Selectors } from '../../../../shared/state/selectors';
import { IVolume } from '../../../../shared/model/volume.model';
import { IProject } from '../../../../shared/model/project.model';
import { IDatabase } from '../../../../shared/model/database.model';
import { ApplicationMode } from '../../../../shared/model/enum/application-mode.model';
import {
  AppWindow,
  Bot,
  Cpu,
  Database,
  ExternalLink,
  Folder,
  HardDrive,
  LayoutDashboard,
  LogOut,
  Plus,
  Rocket,
  Server,
  Settings,
  SlidersHorizontal,
  Trophy,
  User,
  Users,
  WandSparkles,
  Warehouse,
} from 'lucide-angular';
import { OverlayPanel } from 'primeng/overlaypanel';

const PROJECTS_MENU_ITEM = 'Projects';
const APPS_MENU_ITEM = 'Deployments';
const NODES_MENU_ITEM = 'Compute Resources';
const RESOURCES_MENU_ITEM = 'Learn';
const DATA_FACTORY_MENU_ITEM = 'Data Factory';
const AI_FACTORY_MENU_ITEM = 'AI Factory';

@Component({
  selector: 'sm-app-menu',
  templateUrl: './app-menu.component.html',
})
export class AppMenuComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  deployMenu = viewChild(OverlayPanel);

  projects: IProject[] = [];
  apps: IApplication[] = [];
  models: IApplication[] = [];
  nodes: INode[] = [];
  dbs: IDatabase[] = [];
  volumes: IVolume[] = [];

  model: any[] = [];

  private subscription?: Subscription;

  constructor(
    private accountService: AccountService,
    private store: Store,
    private trackerService: TrackerService
  ) {
    this.model.push({
      class: 'uppercase text-500 text-sm',
      items: []
    });
    this.model[0].items.push({
      label: 'Dashboard',
      lucide: LayoutDashboard,
      routerLink: ['']
    });

    this.model[0].items.push({
      label: 'Data Hub',
      lucide: Warehouse,
      routerLink: ['/hub'],
    });
    this.model.push({
      label: PROJECTS_MENU_ITEM,
      class: 'uppercase text-500 text-sm',
      highlight: true,
      items: []
    });
    this.model.push({
      label: APPS_MENU_ITEM,
      class: 'uppercase text-500 text-sm',
      items: []
    });
    this.model.push({
      label: DATA_FACTORY_MENU_ITEM,
      class: 'uppercase text-500 text-sm',
      items: [
        {
          label: 'Wizard',
          lucide: WandSparkles,
          routerLink: ['/data/wizard'],
        },
        // {
        //   label: 'Factual Data',
        //   lucide: FileText,
        //   routerLink: ['/data/factual'],
        // },
      ]
    });
    this.model.push({
      label: AI_FACTORY_MENU_ITEM,
      class: 'uppercase text-500 text-sm',
      items: [
        {
          label: 'Wizard',
          lucide: WandSparkles,
          routerLink: ['/train/wizard'],
        },
        {
          label: 'Training',
          lucide: Rocket,
          routerLink: ['/train/jobs/ray/training'],
        },
        {
          label: 'Fine-Tuning',
          lucide: SlidersHorizontal,
          routerLink: ['/train/jobs/ray/fine-tuning'],
        },
        {
          label: 'Evaluation',
          lucide: Trophy,
          routerLink: ['/train/jobs/tekton/evaluation'],
        },
        {
          label: 'Quantization',
          lucide: Cpu,
          routerLink: ['/train/jobs/tekton/quantization'],
        },
      ]
    });

    this.model.push({
      label: NODES_MENU_ITEM,
      class: 'uppercase text-500 text-sm',
      items: []
    });
    this.model.push({
      label: 'Admin',
      authorities: ['ROLE_ADMIN'],
      class: 'uppercase text-500 text-sm',
      items: [
        {
          label: 'Clusters',
          lucide: Settings,
          routerLink: ['/admin/cluster']
        },
        {
          label: 'System configuration',
          lucide: Settings,
          routerLink: ['/admin/system-configuration'],
          reset: true
        },
        // {
        //   label: 'Gateway Admin',
        //   lucide: Shield,
        //   command: () => {
        //     window.open('https://proxy.densemax.local/ui/', '_blank');
        //   }
        // },
        {
          label: 'Users',
          lucide: Users,
          routerLink: ['/admin/users'],
          reset: true
        },
      ]
    });
    this.model.push({
      label: 'Account',
      class: 'font-bold',
      items: [
        {
          label: 'Profile',
          lucide: User,
          routerLink: ['profile/settings'],
          reset: true
        },
        {
          label: 'Logout',
          lucide: LogOut,
          routerLink: ['/logout']
        }
      ]
    });
    this.model.push({
      label: RESOURCES_MENU_ITEM,
      class: 'uppercase text-500 text-sm',
      items: [
        {
          label: 'User Guide',
          lucide: ExternalLink,
          command: () => {
            window.open('https://docs.surogate.ai/', '_blank');
          }
        },
        {
          label: 'Tutorials',
          lucide: ExternalLink,
          command: () => {
            window.open('https://docs.surogate.ai/', '_blank');
          }
        }
      ]
    });
  }

  findMenuItemByLabel(label: string): any {
    return this.model.find(item => item.label === label);
  }

  async ngOnInit() {
    await this.listenForMessages();

    this.store.select(Selectors.projects)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (projects) => {
          this.projects = [...projects];
          this.findMenuItemByLabel(PROJECTS_MENU_ITEM).items = [
            ...this.projects.map((project: IProject) => {
              return {
                label: project.name,
                lucide: Folder,
                routerLink: ['/projects'],
                noroute: true,
                keepSelection: true,
                queryParams: {id: project.id}
              };
            }),
            {
              label: 'Create...',
              lucide: Plus,
              routerLink: ['/projects/create']
            }
          ];
        }
      });

    this.store.select(Selectors.deployments)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ apps, dbs, volumes }) => {
          this.apps = [...apps];
          this.dbs = [...dbs];
          this.volumes = [...volumes];

          this.findMenuItemByLabel(APPS_MENU_ITEM).items = [
            ...this.apps.map(app => {
              return {
                label: app.name,
                lucide: app.mode === ApplicationMode.MODEL ? Bot : AppWindow,
                routerLink: app.mode === ApplicationMode.MODEL ? ['/models'] : ['/apps'],
                noroute: true,
                queryParams: {id: app.id}
              };
            }),
            ...this.dbs.map(db => {
              return {
                label: db.name,
                lucide: Database,
                routerLink: ['/dbs'],
                noroute: true,
                queryParams: {id: db.id}
              };
            }),
            ...this.volumes.map(volume => {
              return {
                label: volume.name,
                lucide: HardDrive,
                routerLink: ['/volumes'],
                noroute: true,
                queryParams: {id: volume.id}
              };
            }),
            {
              label: 'New...',
              lucide: Plus,
              command: (data: any) => {
                this.deployMenu().show(data.originalEvent);
              }
            }
          ];
        }
      });

    this.store.select(Selectors.nodes)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (nodes) => {
          this.nodes = [...nodes];
          this.findMenuItemByLabel(NODES_MENU_ITEM).items = [
            ...this.nodes.map((node: INode) => {
              return {
                label: node.name,
                lucide: Server,
                routerLink: ['/nodes'],
                noroute: true,
                queryParams: {id: node.id}
              };
            })
          ];
        },
        error: () => {
        }
      });
  }

  async listenForMessages() {
    const user = await lastValueFrom(this.accountService.identity());
    this.subscription = this.trackerService.stomp
      .watch('/topic/message/' + user.login)
      .pipe(
        map(message => {
          return JSON.parse(message.body) as IUpdateMessage;
        })
      ).subscribe((msg) => {
        this.store.dispatch(new UpdateMessageAction(msg));
      });
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    this.destroy$.next();
    this.destroy$.complete();
  }

  protected readonly AppWindow = AppWindow;
  protected readonly Bot = Bot;
  protected readonly Database = Database;
  protected readonly HardDrive = HardDrive;
}
