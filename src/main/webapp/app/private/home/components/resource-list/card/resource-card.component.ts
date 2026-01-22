import {Component, Input} from '@angular/core';
import {CardModule} from 'primeng/card';
import {RouterLink} from '@angular/router';
import {TagModule} from 'primeng/tag';
import {IDatabase} from '../../../../../shared/model/database.model';
import {IApplication} from '../../../../../shared/model/application.model';
import {IResource} from '../resource-list.component';
import {ApplicationStatus} from '../../../../../shared/model/enum/application-status.model';
import {NgIf} from '@angular/common';
import {DatabaseStatus} from '../../../../../shared/model/enum/database-status.model';
import {IVolume} from '../../../../../shared/model/volume.model';
import { AppWindow, Bot, Database, HardDrive, LucideAngularModule } from 'lucide-angular';

@Component({
  standalone: true,
  templateUrl: './resource-card.component.html',
  selector: 'sm-resource-card',
  imports: [
    CardModule,
    RouterLink,
    TagModule,
    NgIf,
    LucideAngularModule,
  ]
})
export class ResourceCardComponent {
  @Input() resource!: IResource

  // Enums - exposed to template
  protected readonly ApplicationStatus = ApplicationStatus;
  protected readonly DatabaseStatus = DatabaseStatus;

  // Icon components - exposed to template
  protected readonly AppWindow = AppWindow;
  protected readonly Bot = Bot;
  protected readonly Database = Database;
  protected readonly HardDrive = HardDrive;

  routerLink() {
    switch (this.resource.type) {
      case 'app': return '/apps';
      case 'model': return '/models';
      case 'db': return '/dbs';
      case 'vol': return '/volumes';
      default: return '/';
    }
  }

  resourceType() {
    switch (this.resource.type) {
      case 'app': return 'Application';
      case 'model': return 'Model';
      case 'db': return 'Database';
      case 'vol': return 'Volume';
      default: return 'Unknown';
    }
  }

  resourceIconComponent() {
    switch (this.resource.type) {
      case 'app': return this.AppWindow;
      case 'model': return this.Bot;
      case 'db': return this.Database;
      case 'vol': return this.HardDrive;
      default: return this.AppWindow;
    }
  }

  app(): IApplication {
    return this.resource.resource as IApplication;
  }

  db(): IDatabase {
    return this.resource.resource as IDatabase;
  }

  vol(): IVolume {
    return this.resource.resource as IVolume;
  }
}
