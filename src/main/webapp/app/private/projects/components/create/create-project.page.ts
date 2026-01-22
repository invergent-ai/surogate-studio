import { Component, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '../../../../shared/components/page/page.component';
import { PageLoadComponent } from '../../../../shared/components/page-load/page-load.component';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { IZone } from '../../../../shared/model/zone.model';
import { revalidateForm } from '../../../../shared/util/form.util';
import { NewProject } from '../../../../shared/model/project.model';
import { ProjectService } from '../../../../shared/service/project.service';
import { displayError } from '../../../../shared/util/error.util';
import { lastValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { Store } from '@ngxs/store';
import { LoadProjectsAction } from '../../../../shared/state/actions';
import { CardModule } from 'primeng/card';
import { Profile } from '../../../../shared/model/enum/profile.model';
import { MenuService } from '../../../layout/service/app-menu.service';
import { LayoutService } from '../../../../shared/service/theme/app-layout.service';
import { Selectors } from '../../../../shared/state/selectors';

@Component({
  standalone: true,
  templateUrl: './create-project.page.html',
  imports: [
    PageComponent,
    PageLoadComponent,
    ButtonModule,
    FormsModule,
    InputTextModule,
    InputTextareaModule,
    ReactiveFormsModule,
    CardModule
  ]
})
export class CreateProjectPage implements OnInit {
  readonly welcomeItems = [
    { title: 'What is Surogate ?', link: 'https://surogate.ai/' },
  ];
  loading = false;
  projectForm: FormGroup;
  defaultZone: IZone;

  constructor(
    private projectService: ProjectService,
    private router: Router,
    private store: Store,
    private menuService: MenuService,
    private layoutService: LayoutService,
    fb: FormBuilder
  ) {
    this.projectForm = fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      description: ['', [Validators.maxLength(250)]]
    });
  }

  ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;

    // Load the default zone from store
    this.store.select(Selectors.zones).subscribe(zones => {
      if (zones?.length > 0) {
        this.defaultZone = zones[0]; // Use the first (and only) zone
      }
    });
  }

  async saveProject() {
    revalidateForm(this.projectForm);
    if (!this.projectForm.valid) {
      return;
    }

    if (!this.defaultZone) {
      displayError(this.store, 'No zone available. Please contact administrator.');
      return;
    }

    const project: NewProject = {
      id: null,
      name: this.projectForm.value.name,
      description: this.projectForm.value.description ?
        this.projectForm.value.description.trim() : '',
      zone: this.defaultZone,
      profile: Profile.GPU // Always GPU
    };

    try {
      await lastValueFrom(this.projectService.create(project));
      this.store.dispatch(new LoadProjectsAction());
      await this.router.navigate(['']);
      this.menuService.reload();
    } catch (e) {
      displayError(this.store, e);
    }
  }
}
