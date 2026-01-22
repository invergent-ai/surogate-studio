import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {InputSwitchModule} from 'primeng/inputswitch';
import SharedModule from '../../../../../../../shared/shared.module';
import {FormGroup, ReactiveFormsModule} from '@angular/forms';

@Component({
  standalone: true,
  selector: 'sm-start-command',
  imports: [
    InputSwitchModule,
    SharedModule,
    ReactiveFormsModule,
  ],
  templateUrl: './start-command.component.html',
  styleUrls: ['./start-command.component.scss'],
})
export class StartCommandComponent implements OnChanges {
  @Input() containerForm!: FormGroup;
  showForm = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes && changes.containerForm) {
      this.showForm = !!(this.containerForm.get('startCommand')?.value || this.containerForm.get('startParameters')?.value);
    }
  }
}
