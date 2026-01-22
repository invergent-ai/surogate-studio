import {Component, computed, effect, input, output, signal} from '@angular/core';
import {DropdownModule} from 'primeng/dropdown';
import {InputNumberModule} from 'primeng/inputnumber';
import {CardComponent} from '../../../../shared/components/card/card.component';
import {TagModule} from 'primeng/tag';
import {CheckboxModule} from 'primeng/checkbox';
import {FormsModule} from '@angular/forms';
import {InputTextModule} from 'primeng/inputtext';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {Bell, ClipboardList} from 'lucide-angular';

@Component({
  selector: 'sm-job-notification-settings',
  standalone: true,
  templateUrl: './job-notification-settings.component.html',
  imports: [
    DropdownModule,
    InputNumberModule,
    CardComponent,
    TagModule,
    CheckboxModule,
    FormsModule,
    InputTextModule,
    InputTextareaModule
  ]
})
export class JobNotificationSettingsComponent {
  selectedNotification = input<string[] | undefined>();
  notifyChange = output<string[]>();

  notifyOnSuccess = computed(()=>this.selectedNotification()?.includes("success"));
  notifyOnFailure = computed(()=>this.selectedNotification()?.includes("failure"));


  toggleSuccess(checked: boolean) {
    const notifications = this.selectedNotification();
    const updated = checked
      ? [...notifications, "success"]
      : notifications.filter(n => n !== "success");
    this.notifyChange.emit(updated);
  }

  toggleFailure(checked: boolean) {
    const notifications = this.selectedNotification();
    const updated = checked
      ? [...notifications, "failure"]
      : notifications.filter(n => n !== "failure");
    this.notifyChange.emit(updated);
  }

  protected readonly ClipboardList = ClipboardList;
  protected readonly Bell = Bell;
}
