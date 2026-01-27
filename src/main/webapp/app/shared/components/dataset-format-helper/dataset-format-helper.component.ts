import { Component, input, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonDirective } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { Copy, HelpCircle, LucideAngularModule } from 'lucide-angular';

export type DatasetFormatType = 'custom_evaluation' | 'conversation' | 'exact_match' | 'judge' | 'hybrid';

interface Turn {
  role: string;
  content: string;
}

interface FormatExample {
  title: string;
  description: string;
  requiredFields: string[];
  optionalFields: string[];
  examples: string[];
}

interface EvalModeExample {
  mode: string;
  description: string;
  columns: string[];
  examples: string[];
}

const FORMAT_EXAMPLES: Record<DatasetFormatType, FormatExample> = {
  exact_match: {
    title: 'Exact Match Format',
    description:
      'Model output is compared directly to the expected answer. Best for MCQ, factual Q&A, or any task with a definitive correct answer.',
    requiredFields: ['instruction', 'answer'],
    optionalFields: [],
    examples: [
      '{"instruction": "What is 2+2?\\nA. 3\\nB. 4\\nC. 5\\nD. 6", "answer": "B"}',
      '{"instruction": "What is the capital of France?", "answer": "Paris"}',
    ],
  },
  judge: {
    title: 'Judge (LLM-as-Judge) Format',
    description:
      'Uses an LLM judge to evaluate response quality against criteria. Best for open-ended questions, explanations, or creative tasks.',
    requiredFields: ['instruction', 'answer'],
    optionalFields: ['judge_criteria'],
    examples: [
      '{"instruction": "Explain how photosynthesis works.", "answer": "Photosynthesis converts sunlight, water, and CO2 into glucose and oxygen.", "judge_criteria": "Evaluate if the response correctly explains the basic process."}',
      '{"instruction": "What are the main causes of climate change?", "answer": "Greenhouse gas emissions, deforestation, and agriculture.", "judge_criteria": "Check if response identifies key causes."}',
    ],
  },
  hybrid: {
    title: 'Hybrid Format',
    description: 'Mixed dataset with per-row evaluation type. Use eval_type column to specify "exact_match" or "judge" for each row.',
    requiredFields: ['instruction', 'answer', 'eval_type'],
    optionalFields: ['judge_criteria'],
    examples: [
      '{"instruction": "What is 2+2?", "answer": "4", "eval_type": "exact_match"}',
      '{"instruction": "Explain machine learning.", "answer": "ML is AI where computers learn from data.", "eval_type": "judge", "judge_criteria": "Evaluate if response captures learning from data."}',
    ],
  },
  custom_evaluation: {
    title: 'Custom Evaluation Dataset Format',
    description: 'Flexible format supporting three evaluation modes. Choose based on your evaluation needs.',
    requiredFields: ['instruction', 'answer'],
    optionalFields: ['eval_type', 'judge_criteria'],
    examples: [],
  },
  conversation: {
    title: 'Conversation Dataset Format',
    description: 'Multi-turn conversations for evaluating context retention, coherence, and response quality across dialogue turns.',
    requiredFields: ['turns', 'expected_final_output'],
    optionalFields: ['category', 'difficulty'],
    examples: [
      '{"turns": [{"role": "user", "content": "What is Python?"}, {"role": "assistant", "content": "Python is a high-level programming language."}, {"role": "user", "content": "What are its main features?"}], "expected_final_output": "Python features include simplicity, readability, extensive libraries, and cross-platform support", "category": "programming", "difficulty": "medium"}',
      '{"turns": [{"role": "user", "content": "Tell me about Rome"}, {"role": "assistant", "content": "Rome is the capital of Italy with rich history."}, {"role": "user", "content": "What are must-see attractions?"}], "expected_final_output": "The Colosseum, Vatican, and Trevi Fountain are popular attractions", "category": "travel", "difficulty": "easy"}',
    ],
  },
};

const EVAL_MODE_EXAMPLES: EvalModeExample[] = [
  {
    mode: 'Exact Match',
    description: 'Direct comparison with expected answer. Best for MCQ, factual Q&A.',
    columns: ['instruction', 'answer'],
    examples: ['{"instruction": "What is 2+2?", "answer": "4"}', '{"instruction": "Capital of France?", "answer": "Paris"}'],
  },
  {
    mode: 'Judge (LLM-as-Judge)',
    description: 'LLM evaluates response quality. Best for open-ended questions.',
    columns: ['instruction', 'answer', 'judge_criteria'],
    examples: [
      '{"instruction": "Explain photosynthesis.", "answer": "Plants convert sunlight to glucose.", "judge_criteria": "Must mention sunlight and glucose."}',
    ],
  },
  {
    mode: 'Hybrid',
    description: 'Mix both modes. Specify eval_type per row.',
    columns: ['instruction', 'answer', 'eval_type', 'judge_criteria'],
    examples: [
      '{"instruction": "What is 2+2?", "answer": "4", "eval_type": "exact_match"}',
      '{"instruction": "Explain ML.", "answer": "Computers learn from data.", "eval_type": "judge", "judge_criteria": "Mentions learning from data."}',
    ],
  },
];

@Component({
  selector: 'sm-dataset-format-helper',
  standalone: true,
  imports: [CommonModule, ButtonDirective, TooltipModule, DialogModule, TableModule, LucideAngularModule],
  template: `
    <button
      pButton
      type="button"
      size="small"
      [text]="true"
      class="text-500"
      (click)="dialogVisible.set(true)"
      pTooltip="View dataset format"
      tooltipPosition="top"
    >
      <div class="flex align-items-center gap-1">
        <i-lucide [img]="HelpCircle" class="w-1rem h-1rem"></i-lucide>
        <span class="text-xs">Dataset format</span>
      </div>
    </button>

    <p-dialog
      [header]="format().title"
      [(visible)]="dialogVisible"
      [modal]="true"
      [style]="{ width: '90vw', maxWidth: '1100px' }"
      [draggable]="false"
      [resizable]="false"
    >
      <div class="text-sm">
        <p class="text-600 mt-0 mb-4">{{ format().description }}</p>

        <!-- Custom evaluation: show all 3 modes -->
        @if (formatType() === 'custom_evaluation') {
          @for (evalMode of evalModeExamples; track evalMode.mode) {
            <div class="mb-4 p-3 border-1 border-round surface-border">
              <div class="flex align-items-center justify-content-between mb-2">
                <span class="font-semibold text-700">{{ evalMode.mode }}</span>
                <span class="text-xs text-500">{{ evalMode.description }}</span>
              </div>

              <p-table [value]="parseExamples(evalMode.examples)" styleClass="p-datatable-sm p-datatable-gridlines" [scrollable]="true">
                <ng-template pTemplate="header">
                  <tr>
                    <th class="bg-gray-100 text-xs font-semibold" style="width: 40px">#</th>
                    @for (col of evalMode.columns; track col) {
                      <th class="bg-gray-100 text-xs font-semibold">{{ col }}</th>
                    }
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-row let-rowIndex="rowIndex">
                  <tr>
                    <td class="text-center text-500 text-xs">{{ rowIndex + 1 }}</td>
                    @for (col of evalMode.columns; track col) {
                      <td class="text-xs" style="max-width: 280px; white-space: pre-wrap; word-break: break-word;">
                        <span class="text-700">{{ row[col] ?? '—' }}</span>
                      </td>
                    }
                  </tr>
                </ng-template>
              </p-table>

              <div class="mt-2">
                <div class="flex align-items-center justify-content-between mb-1">
                  <span class="text-xs text-500">JSONL</span>
                  <button
                    pButton
                    type="button"
                    size="small"
                    [text]="true"
                    class="text-xs p-1"
                    (click)="
                      copyToClipboard(
                        evalMode.examples.join(
                          '
'
                        )
                      )
                    "
                    pTooltip="Copy"
                    tooltipPosition="left"
                  >
                    <i-lucide [img]="Copy" class="w-0.75rem h-0.75rem"></i-lucide>
                  </button>
                </div>
                <div class="bg-gray-900 text-gray-100 p-2 border-round overflow-auto" style="max-height: 80px;">
                  <pre class="m-0 text-xs" style="white-space: pre-wrap; word-break: break-all;">{{
                    evalMode.examples.join(
                      '
'
                    )
                  }}</pre>
                </div>
              </div>
            </div>
          }
        } @else {
          <!-- Other formats: single table -->
          <div class="flex gap-4 mb-4">
            <div>
              <div class="font-medium text-700 mb-2">Required fields</div>
              <div class="flex flex-wrap gap-1">
                @for (field of format().requiredFields; track field) {
                  <span class="px-2 py-1 bg-blue-100 text-blue-700 border-round text-xs font-medium">{{ field }}</span>
                }
              </div>
            </div>
            @if (format().optionalFields?.length) {
              <div>
                <div class="font-medium text-700 mb-2">Optional fields</div>
                <div class="flex flex-wrap gap-1">
                  @for (field of format().optionalFields; track field) {
                    <span class="px-2 py-1 bg-gray-100 text-gray-600 border-round text-xs">{{ field }}</span>
                  }
                </div>
              </div>
            }
          </div>

          <div class="mb-4">
            <div class="flex align-items-center justify-content-between mb-2">
              <span class="font-medium text-700">Example rows</span>
              <span class="text-xs text-500">Each row = 1 line in your JSONL file</span>
            </div>

            <p-table [value]="parsedRows()" styleClass="p-datatable-sm p-datatable-gridlines" [scrollable]="true">
              <ng-template pTemplate="header">
                <tr>
                  <th class="bg-gray-100 text-xs font-semibold" style="width: 40px">#</th>
                  @for (col of columns(); track col) {
                    <th class="bg-gray-100 text-xs font-semibold" [class.text-blue-700]="isRequired(col)">
                      {{ col }}
                      @if (isRequired(col)) {
                        <span class="text-red-500">*</span>
                      }
                    </th>
                  }
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-row let-rowIndex="rowIndex">
                <tr>
                  <td class="text-center text-500 text-xs">{{ rowIndex + 1 }}</td>
                  @for (col of columns(); track col) {
                    <td class="text-xs" style="max-width: 300px; white-space: pre-wrap; word-break: break-word;">
                      @if (col === 'turns' && isTurnsArray(row[col])) {
                        <div class="flex flex-column gap-1">
                          @for (turn of asTurns(row[col]); track $index) {
                            <div class="flex gap-2 align-items-start">
                              <span
                                class="px-1 border-round text-xs font-medium flex-shrink-0"
                                [class.bg-blue-100]="turn.role === 'user'"
                                [class.text-blue-700]="turn.role === 'user'"
                                [class.bg-green-100]="turn.role === 'assistant'"
                                [class.text-green-700]="turn.role === 'assistant'"
                              >
                                {{ turn.role }}
                              </span>
                              <span class="text-700">{{ turn.content }}</span>
                            </div>
                          }
                        </div>
                      } @else {
                        <span class="text-700">{{ row[col] ?? '—' }}</span>
                      }
                    </td>
                  }
                </tr>
              </ng-template>
            </p-table>
          </div>

          <div>
            <div class="flex align-items-center justify-content-between mb-2">
              <span class="font-medium text-700">Raw JSONL</span>
              <button
                pButton
                type="button"
                size="small"
                severity="secondary"
                outlined
                (click)="copyExamples()"
                pTooltip="Copy to clipboard"
                tooltipPosition="left"
              >
                <div class="flex align-items-center gap-1">
                  <i-lucide [img]="Copy" class="w-0.875rem h-0.875rem"></i-lucide>
                  <span>Copy</span>
                </div>
              </button>
            </div>
            <div class="bg-gray-900 text-gray-100 p-3 border-round overflow-auto" style="max-height: 150px;">
              <pre class="m-0 text-xs" style="white-space: pre-wrap; word-break: break-all;">{{ formattedExamples() }}</pre>
            </div>
            <small class="text-500 mt-2 block">Each line is a separate JSON object. Save as .jsonl file.</small>
          </div>
        }
      </div>

      <ng-template pTemplate="footer">
        <button pButton type="button" label="Close" severity="secondary" (click)="dialogVisible.set(false)"></button>
      </ng-template>
    </p-dialog>
  `,
})
export class DatasetFormatHelperComponent {
  formatType = input.required<DatasetFormatType>();

  dialogVisible = signal(false);

  protected readonly Copy = Copy;
  protected readonly HelpCircle = HelpCircle;
  protected readonly evalModeExamples = EVAL_MODE_EXAMPLES;

  format = computed(() => FORMAT_EXAMPLES[this.formatType()] || FORMAT_EXAMPLES['custom_evaluation']);

  parsedRows = computed(() => this.parseExamples(this.format().examples));

  columns = computed(() => {
    const format = this.format();
    return [...format.requiredFields, ...format.optionalFields];
  });

  formattedExamples = computed(() => this.format().examples.join('\n'));

  parseExamples(examples: string[]): Record<string, unknown>[] {
    return examples.map(jsonStr => {
      try {
        return JSON.parse(jsonStr);
      } catch {
        return {};
      }
    });
  }

  isRequired(field: string): boolean {
    return this.format().requiredFields.includes(field);
  }

  isTurnsArray(value: unknown): value is Turn[] {
    return Array.isArray(value) && value.length > 0 && typeof value[0] === 'object' && 'role' in value[0];
  }

  asTurns(value: unknown): Turn[] {
    return value as Turn[];
  }

  copyExamples(): void {
    this.copyToClipboard(this.formattedExamples());
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }
}
