import {FormArray, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {takeUntil} from 'rxjs/operators';
import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SharedModule} from 'primeng/api';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {AccordionModule} from 'primeng/accordion';
import {Subject} from 'rxjs';
import {NewAnnotation} from "../../../../../shared/model/annotation.model";
import {ApplicationFormService} from "../../../../../shared/service/form/application-form.service";
import {LayoutService} from "../../../../../shared/service/theme/app-layout.service";

@Component({
  selector: 'sm-annotations',
  templateUrl: './annotations.component.html',
  styleUrls: ['./annotations.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    InputTextModule,
    ButtonModule,
    AccordionModule
  ]
})
export class AnnotationsComponent implements OnInit, OnDestroy {
  @Input() applicationForm!: FormGroup;
  @Output() annotationsUpdated = new EventEmitter<NewAnnotation[]>();

  private destroy$ = new Subject<void>();

  constructor(
    public layoutService: LayoutService,
    private applicationFormService: ApplicationFormService) {}

  get annotations(): FormArray {
    return this.applicationForm.get('annotations') as FormArray;
  }

  addAnnotation(): void {
    const annotations = this.applicationForm.get('annotations') as FormArray;
    annotations.push(this.applicationFormService.createAnnotationForm());
    this.emitUpdatedAnnotations();
  }

  removeAnnotation(index: number): void {
    const annotations = this.applicationForm.get('annotations') as FormArray;
    annotations.removeAt(index);
    this.emitUpdatedAnnotations();
  }

  private emitUpdatedAnnotations(): void {
    const annotationsArray = this.applicationForm.get('annotations') as FormArray;
    const annotations = annotationsArray.controls.map(control => ({
      id: null,
      key: control.get('key')?.value,
      value: control.get('value')?.value
    }));

    this.annotationsUpdated.emit(annotations);
  }

  ngOnInit(): void {
    if (!this.applicationForm.contains('annotations')) {
      this.applicationForm.addControl('annotations', new FormArray([]));
    }

    const annotationsArray = this.applicationForm.get('annotations') as FormArray;
    const existingAnnotations = this.applicationForm.get('annotations')?.value || [];

    // Clear existing values
    while (annotationsArray.length) {
      annotationsArray.removeAt(0);
    }

    // Add existing or initialize empty
    if (existingAnnotations.length) {
      existingAnnotations.forEach((ann: NewAnnotation) => {
        annotationsArray.push(this.applicationFormService.createAnnotationForm(ann));
      });
    }

    annotationsArray.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.emitUpdatedAnnotations();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
