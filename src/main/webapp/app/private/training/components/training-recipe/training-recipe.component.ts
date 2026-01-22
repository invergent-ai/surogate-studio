import {Component, input, InputSignal, signal} from '@angular/core';
import {CardComponent} from '../../../../shared/components/card/card.component';
import {DropdownModule} from 'primeng/dropdown';
import {InputNumberModule} from 'primeng/inputnumber';
import {ButtonDirective} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {LucideAngularModule, Settings2, SlidersHorizontal} from 'lucide-angular';
import {
  TrainingOptimizerType,
  TrainingPrecisionType,
  TrainingSchedulerType
} from '../../../../shared/model/training.model';
import {CheckboxModule} from 'primeng/checkbox';
import {TagModule} from 'primeng/tag';
import {FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {NgIf} from '@angular/common';
import {LabelTooltipComponent} from "../../../../shared/components/label-tooltip/label-tooltip.component";
import {
  COOLDOWN_STEPS,
  DEBUG_MEMORY_BREAKDOWN,
  DEBUG_TIME_BREAKDOWN,
  EVAL_STEPS,
  FINAL_LR_FRACTION,
  GRADIENT_ACCUMULATION_STEPS,
  GRADIENT_CHECKPOINTING,
  LEARNING_RATE,
  LOGGING_STEPS,
  LR_SCHEDULER,
  MAX_GRAD_NORM,
  MAX_STEPS,
  MICRO_BATCH_SIZE,
  NUM_EPOCHS,
  OPTIMIZER,
  RECIPE,
  SAMPLE_PACKING,
  SEQUENCE_LENGTH,
  SKIP_QUANT_FIRST_LAYERS,
  SKIP_QUANT_LAST_LAYERS,
  TRAIN_ON_INPUTS,
  VALIDATION_SPLIT_RATIO,
  WARMUP_RATIO,
  WARMUP_STEPS,
  WEIGHT_DECAY,
  ZERO_LEVEL
} from "../../tooltips";
import {filterDouble, filterSciNumber} from "../../../../shared/util/validators.util";

@Component({
  selector: 'sm-training-recipe',
  standalone: true,
  templateUrl: './training-recipe.component.html',
  imports: [
    CardComponent,
    DropdownModule,
    InputNumberModule,
    ButtonDirective,
    InputTextModule,
    LucideAngularModule,
    CheckboxModule,
    TagModule,
    FormsModule,
    NgIf,
    ReactiveFormsModule,
    LabelTooltipComponent
  ]
})
export class TrainingRecipeComponent {
  trainingForm: InputSignal<FormGroup> = input<FormGroup>();
  advanced = signal(false);

  trainingPrecisions: any[] = [
    { label: 'BF16', value: TrainingPrecisionType.BF16 },
    { label: 'FP8', value: TrainingPrecisionType.FP8 },
    { label: 'FP4', value: TrainingPrecisionType.FP4 },
  ];
  optimizers: any[] = [
    { label: 'AdamW', value: TrainingOptimizerType.AdamW },
    { label: 'SGD', value: TrainingOptimizerType.SGD },
    { label: 'NorMuon', value: TrainingOptimizerType.NorMuon }
  ];
  schedulers: any[] = [
    { label: 'Cosine', value: TrainingSchedulerType.Cosine },
    { label: 'Linear', value: TrainingSchedulerType.Linear },
    { label: 'WSD', value: TrainingSchedulerType.WSD },
    { label: 'Constant', value: TrainingSchedulerType.Constant },
  ];

  protected readonly Settings2 = Settings2;
  protected readonly SlidersHorizontal = SlidersHorizontal;

  protected readonly NUM_EPOCHS = NUM_EPOCHS;
  protected readonly MICRO_BATCH_SIZE = MICRO_BATCH_SIZE;
  protected readonly GRADIENT_ACCUMULATION_STEPS = GRADIENT_ACCUMULATION_STEPS;
  protected readonly LEARNING_RATE = LEARNING_RATE;
  protected readonly SEQUENCE_LENGTH = SEQUENCE_LENGTH;
  protected readonly OPTIMIZER = OPTIMIZER;
  protected readonly GRADIENT_CHECKPOINTING = GRADIENT_CHECKPOINTING;
  protected readonly TRAIN_ON_INPUTS = TRAIN_ON_INPUTS;
  protected readonly WEIGHT_DECAY = WEIGHT_DECAY;
  protected readonly MAX_STEPS = MAX_STEPS;
  protected readonly SAMPLE_PACKING = SAMPLE_PACKING;
  protected readonly VALIDATION_SPLIT_RATIO = VALIDATION_SPLIT_RATIO;
  protected readonly EVAL_STEPS = EVAL_STEPS;
  protected readonly LOGGING_STEPS = LOGGING_STEPS;
  protected readonly MAX_GRAD_NORM = MAX_GRAD_NORM;
  protected readonly LR_SCHEDULER = LR_SCHEDULER;
  protected readonly WARMUP_STEPS = WARMUP_STEPS;
  protected readonly WARMUP_RATIO = WARMUP_RATIO;
  protected readonly COOLDOWN_STEPS = COOLDOWN_STEPS;
  protected readonly FINAL_LR_FRACTION = FINAL_LR_FRACTION;
  protected readonly SKIP_QUANT_FIRST_LAYERS = SKIP_QUANT_FIRST_LAYERS;
  protected readonly SKIP_QUANT_LAST_LAYERS = SKIP_QUANT_LAST_LAYERS;
  protected readonly DEBUG_TIME_BREAKDOWN = DEBUG_TIME_BREAKDOWN;
  protected readonly DEBUG_MEMORY_BREAKDOWN = DEBUG_MEMORY_BREAKDOWN;
  protected readonly RECIPE = RECIPE;
  protected readonly ZERO_LEVEL = ZERO_LEVEL;

  protected readonly filterSciNumber = filterSciNumber;
  protected readonly filterDouble = filterDouble;
}
