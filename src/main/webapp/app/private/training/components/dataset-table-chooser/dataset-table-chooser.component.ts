import {Component, computed, input, OnChanges, OnInit, signal} from '@angular/core';
import {LabelTooltipComponent} from '../../../../shared/components/label-tooltip/label-tooltip.component';
import {Database, LucideAngularModule, Plus, Trash} from 'lucide-angular';
import {PrimeTemplate} from 'primeng/api';
import {TableModule, TableRowExpandEvent} from 'primeng/table';
import {
  DATASET_CONVERSATION_FIELD_MESSAGES,
  DATASET_CONVERSATION_FIELD_TOOLS,
  DATASET_FORMAT,
  DATASET_INPUT_FIELD,
  DATASET_INSTRUCTION_FIELD,
  DATASET_INSTRUCTION_PROMPT_FORMAT,
  DATASET_INSTRUCTION_PROMPT_FORMAT_NO_INPUT,
  DATASET_MAX_RECORDS,
  DATASET_OUTPUT_FIELD,
  DATASET_REF,
  DATASET_REPOSITORY,
  DATASET_SPLIT,
  DATASET_SYSTEM_PROMPT_FIELD,
  DATASET_SYSTEM_PROMPT_TYPE,
  DATASET_TEXT_COLUMN
} from '../../tooltips';
import {FormArray, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {ButtonDirective} from 'primeng/button';
import {CardComponent} from '../../../../shared/components/card/card.component';
import {InputNumberModule} from 'primeng/inputnumber';
import {v4 as uuidv4} from 'uuid';
import {InputTextModule} from 'primeng/inputtext';
import {FieldsetModule} from 'primeng/fieldset';
import {DropdownModule} from 'primeng/dropdown';
import {RepoSelectorComponent} from '../../../../shared/components/repo-selector/repo-selector.component';
import {InputGroupModule} from 'primeng/inputgroup';
import {InputTextareaModule} from 'primeng/inputtextarea';
import {RefSelection, RefSelectorComponent} from '../../../hub/components/ref-selector.component';
import {NgIf} from '@angular/common';

export type DatasetTask = 'pretrain' | 'sft';
export const newDatasetForm = (id: string, type: string) => new FormGroup({
  id: new FormControl<string>(id, [Validators.required]),
  repoId: new FormControl<string>(null, [Validators.required]),
  ref: new FormControl<RefSelection>(null, [Validators.required]),
  path: new FormControl<string>(null, []),
  type: new FormControl<string>(type, [Validators.required]),
  subset: new FormControl<string>(null, []),
  split: new FormControl<string>(null, []),
  samples: new FormControl<string>(null),
  // text format
  textField: new FormControl<string>(null),
  // instruction format
  instructionField: new FormControl<string>(null),
  inputField: new FormControl<string>(null),
  outputField: new FormControl<string>(null),
  systemPromptType: new FormControl<string>(null),
  systemPromptField: new FormControl<string>(null),
  systemPrompt: new FormControl<string>(null),
  promptFormat: new FormControl<string>(null),
  promptFormatNoInput: new FormControl<string>(null),
  // conversation format
  messagesField: new FormControl<string>(null),
  systemField: new FormControl<string>(null),
  toolsField: new FormControl<string>(null),
  messagePropertyMappingsRole: new FormControl<string>(null),
  messagePropertyMappingsContent: new FormControl<string>(null)
});

@Component({
  selector: 'sm-dataset-table-chooser',
  standalone: true,
  templateUrl: './dataset-table-chooser.component.html',
  imports: [
    LabelTooltipComponent,
    LucideAngularModule,
    PrimeTemplate,
    TableModule,
    ButtonDirective,
    CardComponent,
    InputNumberModule,
    ReactiveFormsModule,
    InputTextModule,
    FieldsetModule,
    DropdownModule,
    RepoSelectorComponent,
    InputGroupModule,
    InputTextareaModule,
    RefSelectorComponent,
    FormsModule,
    NgIf,
  ]
})
export class DatasetTableChooserComponent implements OnInit, OnChanges {
  task = input.required<DatasetTask>();
  title = input('Training datasets');
  formArray = input<FormArray>();
  multiple = input(false);

  formArrayForDisplay = signal<FormGroup[]>([]);
  currentForm = signal<FormGroup>(null);
  expandedRows = signal<Record<string, boolean>>({});
  dsFormats = computed(() => {
    const task = this.task();
    if (task === 'pretrain') {
      return [
        { label: 'Pre-training', value: "text" }
      ];
    } else if (task === 'sft') {
      return [
        { label: 'Continued pre-training', value: "text" },
        { label: "Instruction Tuning", value: "instruction" },
        { label: "Conversation", value: "conversation" },
      ];
    } else {
      return [];
    }
  });

  ngOnInit() {
    this.initFormArraySubscription();
  }

  ngOnChanges() {
    this.initFormArraySubscription();
  }

  private initFormArraySubscription() {
    const fa = this.formArray();
    if (!fa) {
      this.formArrayForDisplay.set([]);
      return;
    }
    // Avoid multiple subscriptions
    (fa as any).__dsChooserSubscribed ||= (
      fa.valueChanges.subscribe(() => {
        this.formArrayForDisplay.set(this.formArrayAsArray());
      })
    );
    // Initial population
    this.formArrayForDisplay.set(this.formArrayAsArray());
  }

  add() {
    const id = uuidv4();
    const form = newDatasetForm(id, this.task() === 'pretrain' ? 'text' : 'instruction');
    this.formArray().push(form);
    this.formArrayForDisplay.set(this.formArrayAsArray());
    this.currentForm.set(form);
    this.expandedRows.set({ [id]: true });
  }

  remove(dataset: FormGroup) {
    let idx = this.indexOfDataset(dataset);
    if (idx > -1) {
      this.formArray().removeAt(idx);
    }
    this.formArrayForDisplay.set(this.formArrayAsArray());
    if (this.currentForm() && this.currentForm().value.id === dataset.value.id) {
      this.currentForm.set(null);
      this.expandedRows.set({});
    }
  }

  onRowExpand(event: TableRowExpandEvent) {
    const dataset = event.data;
    if (dataset) {
      this.currentForm.set(this.formArray().at(this.indexOfDataset(dataset)) as FormGroup);
    } else {
      this.currentForm.set(null);
    }
  }

  onRowCollapse(_: TableRowExpandEvent) {
    this.currentForm.set(null);
    this.expandedRows.set({});
  }

  uniqFormEntry = (formGroup: FormGroup): string => {
    return formGroup ? formGroup.value.id : null;
  };

  dsFormatLabel = (format: string): string => {
    return format ? this.dsFormats().filter(ds => ds.value === format)[0]?.label : '';
  };

  private formArrayAsArray(): FormGroup[] {
    if (!this.formArray()) {
      return [];
    }

    const arr = new Array<FormGroup>(this.formArray().length);
    for (let i = 0; i < arr.length; i++) {
      arr[i] = this.formArray().at(i) as FormGroup;
    }
    return arr;
  }

  private indexOfDataset(dataset: FormGroup): number {
    for (let i=0; i<this.formArray().length; i++) {
      const form = this.formArray().at(i) as FormGroup;
      if (form.value.id === dataset.value.id) {
        return i;
      }
    }
    return -1;
  }

  readonly systemPromptType = [
    {label: 'None', value: "none"},
    {label: "Field", value: "field"},
    {label: "Fixed", value: "fixed"},
  ];

  protected readonly DATASET_SPLIT = DATASET_SPLIT;
  protected readonly DATASET_REPOSITORY = DATASET_REPOSITORY;
  protected readonly Trash = Trash;
  protected readonly Database = Database;
  protected readonly Plus = Plus;
  protected readonly DATASET_FORMAT = DATASET_FORMAT;
  protected readonly DATASET_SYSTEM_PROMPT_TYPE = DATASET_SYSTEM_PROMPT_TYPE;
  protected readonly DATASET_REF = DATASET_REF;
  protected readonly DATASET_TEXT_COLUMN = DATASET_TEXT_COLUMN;
  protected readonly DATASET_SYSTEM_PROMPT_FIELD = DATASET_SYSTEM_PROMPT_FIELD;
  protected readonly DATASET_INSTRUCTION_FIELD = DATASET_INSTRUCTION_FIELD;
  protected readonly DATASET_INPUT_FIELD = DATASET_INPUT_FIELD;
  protected readonly DATASET_OUTPUT_FIELD = DATASET_OUTPUT_FIELD;
  protected readonly DATASET_INSTRUCTION_PROMPT_FORMAT = DATASET_INSTRUCTION_PROMPT_FORMAT;
  protected readonly DATASET_INSTRUCTION_PROMPT_FORMAT_NO_INPUT = DATASET_INSTRUCTION_PROMPT_FORMAT_NO_INPUT;
  protected readonly DATASET_CONVERSATION_FIELD_MESSAGES = DATASET_CONVERSATION_FIELD_MESSAGES;
  protected readonly DATASET_CONVERSATION_FIELD_TOOLS = DATASET_CONVERSATION_FIELD_TOOLS;
  protected readonly DATASET_MAX_RECORDS = DATASET_MAX_RECORDS;
}
