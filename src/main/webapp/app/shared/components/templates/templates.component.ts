import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { NgForOf, NgIf } from '@angular/common';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { Router, RouterLink } from '@angular/router';
import { lastValueFrom } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { BadgeModule } from 'primeng/badge';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { DropdownModule } from 'primeng/dropdown';
import { FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { uniq } from 'lodash';
import { ImageModule } from 'primeng/image';
import { AvatarModule } from 'primeng/avatar';
import { PageLoadComponent } from '../page-load/page-load.component';
import { IAppTemplate } from '../../model/app-template.model';
import { AppTemplateService } from '../../service/app-template.service';
import { ApplicationService } from '../../service/application.service';
import { AccountService } from '../../service/account.service';
import { truncate } from '../../util/display.util';
import { MenuService } from '../../../private/layout/service/app-menu.service';
import { ApplicationMode } from '../../model/enum/application-mode.model';
import { ButtonDirective } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { BookOpen, CodeXml, LucideAngularModule, Rocket, Server } from 'lucide-angular';
import { LayoutService } from '../../service/theme/app-layout.service';
import { Authority } from '../../../config/constant/authority.constants';
import { Account } from '../../model/account.model';
import { AppTemplateFormService } from '../../service/form/app-template-form.service';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';
import { RefSelection, RefSelectorComponent } from '../../../private/hub/components/ref-selector.component';
import { RepoSelectorComponent } from '../repo-selector/repo-selector.component';
import { ContainerType } from '../../model/enum/container-type.model';
import { RadioButtonModule } from 'primeng/radiobutton';
import { LakeFsService } from '../../service/lake-fs.service';
import { MessagesModule } from 'primeng/messages';
import { createAppFromTemplate } from '../../util/template.util';

interface ICategory {
  name: string;
  count: number;
}

interface IDropdownOption {
  label: string;
  value: string;
}

@Component({
  standalone: true,
  selector: 'sm-app-templates',
  imports: [
    ProgressSpinnerModule,
    PageLoadComponent,
    NgForOf,
    RouterLink,
    BadgeModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    DropdownModule,
    FormsModule,
    NgIf,
    ImageModule,
    AvatarModule,
    PageLoadComponent,
    ButtonDirective,
    CardModule,
    TagModule,
    LucideAngularModule,
    DialogModule,
    InputNumberModule,
    InputTextareaModule,
    ReactiveFormsModule,
    ConfirmDialogModule,
    TooltipModule,
    OverlayPanelModule,
    RefSelectorComponent,
    RepoSelectorComponent,
    RadioButtonModule,
    MessagesModule
  ],
  templateUrl: './templates.component.html',
})
export class TemplatesComponent implements OnInit {
  @Input() withCustomApp: boolean;
  @Input() public: boolean;
  @Input() title: string;
  @ViewChild('searchBar') searchBar!: ElementRef;
  @ViewChild('deploy') deploy!: OverlayPanel;

  readonly defaultTemplateApp: IAppTemplate =
    {
      id: '7455c54d-7472-4c81-905b-921c1fd71fd2',
      name: 'My own application',
      description: 'Create an application from scratch'
    };

  categories: ICategory[] = [];
  templates: IAppTemplate[];
  displayTemplates: IAppTemplate[] = [];
  loading = false;

  selectedTemplate: IAppTemplate;
  selectedRepository: string;
  selectedSource: string;
  selectedHfToken: string;
  selectedHfModelName: string;
  selectedRef: RefSelection;
  repoExists: boolean;

  // Filter properties
  selectedCategory: string | null = null;
  selectedSortBy: string | null;
  currentSearchTerm: string = '';
  currentSelectedCategory: string = 'All';
  // Dropdown options
  categoryOptions: IDropdownOption[] = [];
  sortOptions: IDropdownOption[] = [
    {label: 'Name (A-Z)', value: 'name-asc'},
    {label: 'Name (Z-A)', value: 'name-desc'},
    {label: 'Category (A-Z)', value: 'category-asc'},
    {label: 'Category (Z-A)', value: 'category-desc'}
  ];
  user: Account;
  isAdmin = false;
  templateForm: FormGroup;
  displayCreateDialog = false;

  protected readonly truncate = truncate;

  constructor(private appTemplateService: AppTemplateService,
              private applicationService: ApplicationService,
              private layoutService: LayoutService,
              private accountService: AccountService,
              private menuService: MenuService,
              private appTemplateFormService: AppTemplateFormService,
              private confirmationService: ConfirmationService,
              private router: Router,
              private lakeFsService: LakeFsService) {
    this.templateForm = this.appTemplateFormService.createAppTemplateFormGroup();
  }

  async ngOnInit() {
    this.layoutService.state.helpItems = this.welcomeItems;
    this.user = await lastValueFrom(this.accountService.identity(true));
    this.isAdmin = this.accountService.hasAnyAuthority([Authority.ADMIN]);
    await this.initTemplates();
  }

  async initTemplates() {
    let templates = await lastValueFrom(
      this.appTemplateService.query()
        .pipe(map((res: HttpResponse<IAppTemplate[]>) => res.body ?? []))
    );

    this.templates = templates.sort((a, b) => (a.zorder || 0) - (b.zorder || 0));
    this.displayTemplates = [...this.templates];

    // Initialize categories for sidebar
    const categories = uniq(templates.map(t => t.category).filter(Boolean));
    this.categories = categories.map(category => {
      return {
        name: category!,
        count: templates.filter(t => t.category === category).length
      };
    });
    this.categories.sort((a, b) => a.name.localeCompare(b.name));
    this.categories.unshift({'name': 'All', count: this.templates.length});

    // Initialize dropdown options
    this.initializeCategoryOptions();

    this.applyFilters();
  }

  private initializeCategoryOptions() {
    // Get all hashtags from all templates
    const allHashtags = this.templates
      .map(t => this.getHashtagsArray(t.hashtags))
      .flat()
      .filter(Boolean); // Remove empty values

    // Get unique hashtags
    const uniqueHashtags = uniq(allHashtags);

    this.categoryOptions = uniqueHashtags.map(hashtag => ({
      label: hashtag,
      value: hashtag
    }));

    this.categoryOptions.sort((a, b) => a.label.localeCompare(b.label));
  }

  private applyFilters() {
    let filtered = [...this.templates];

    // Apply search filter
    if (this.currentSearchTerm) {
      const searchLower = this.currentSearchTerm.toLowerCase();
      filtered = filtered.filter(t =>
        (t.name && t.name.toLowerCase().includes(searchLower)) ||
        (t.description && t.description.toLowerCase().includes(searchLower)) ||
        (t.category && t.category.toLowerCase().includes(searchLower)) ||
        (t.hashtags && t.hashtags.toLowerCase().includes(searchLower))
      );
    }

    // Apply category filter (now filtering by hashtags)
    if (this.selectedCategory) {
      filtered = filtered.filter(t => {
        const templateHashtags = this.getHashtagsArray(t.hashtags);
        return templateHashtags.includes(this.selectedCategory!);
      });
    }

    // Apply sorting
    if (this.selectedSortBy) {
      filtered = this.applySorting(filtered, this.selectedSortBy);
    } else {
      filtered.sort((a, b) => (a.zorder || 0) - (b.zorder || 0));
    }

    if (this.currentSelectedCategory && this.currentSelectedCategory !== 'All') {
      filtered = filtered.filter(t => t.category === this.currentSelectedCategory);
    }

    this.displayTemplates = filtered;
  }

  private applySorting(templates: IAppTemplate[], sortBy: string): IAppTemplate[] {
    const sorted = [...templates];

    switch (sortBy) {
      case 'name-asc':
        return sorted.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
      case 'name-desc':
        return sorted.sort((a, b) => (b.name || '').localeCompare(a.name || ''));
      case 'category-asc':
        return sorted.sort((a, b) => (a.category || '').localeCompare(b.category || ''));
      case 'category-desc':
        return sorted.sort((a, b) => (b.category || '').localeCompare(a.category || ''));
      default:
        return sorted.sort((a, b) => (a.zorder || 0) - (b.zorder || 0));
    }
  }

  search($event: KeyboardEvent) {
    this.currentSearchTerm = ($event.target as HTMLInputElement).value;
    this.applyFilters();
  }

  onSortChange() {
    this.applyFilters();
  }

  filterByCategory(category: string) {
    // Track the currently selected category
    this.currentSelectedCategory = category;

    // Reset inline filters when using sidebar category filter
    this.selectedCategory = null;
    this.selectedSortBy = null;
    this.currentSearchTerm = '';
    // Clear search input
    this.searchBar.nativeElement.value = '';

    if (category === 'All') {
      this.displayTemplates = [...this.templates];
    } else {
      this.displayTemplates = this.templates.filter(t => t.category === category);
    }
  }

  protected getHashtagsArray(hashtags: string | null): string[] {
    if (!hashtags) return [];

    return hashtags
      .split(/[,;#\s]+/)
      .filter(tag => tag.trim().length > 0)
      .map(tag => tag.trim().toLowerCase())
      .slice(0, 5);
  }

  async createApp(template: IAppTemplate) {
    if (this.public) {
      await this.router.navigate(['/login']);
      return;
    }

    await createAppFromTemplate(template, this.router, this.menuService, this.applicationService, this.user.defaultProject);
  }

  async deployTemplate(event: Event, template: IAppTemplate) {
    event.preventDefault();
    event.stopPropagation();

    this.selectedTemplate = template;
    const templateJSON = JSON.parse(template.template);
    const extraConfig = templateJSON.extraConfig ? JSON.parse(templateJSON.extraConfig) : null;
    this.selectedSource = extraConfig?.source ?? 'hf';
    this.selectedHfToken = extraConfig?.hfToken;
    this.selectedHfModelName = extraConfig?.hfModelName;
    this.selectedRepository = extraConfig?.modelName?.split('/')[0];
    this.selectedRef = {id: extraConfig?.modelName?.split('/')[1], type: 'branch'};

    const repos = await lastValueFrom(this.lakeFsService.listRepositories());
    if (repos) {
      const frepos = repos.filter(repo => repo.metadata?.displayName === this.selectedRepository);
      this.repoExists = !!(frepos?.length);
      if (this.repoExists) {
        this.selectedRepository = frepos[0].id;
      }
    }

    this.deploy.toggle(event);
  }

  async doDeploy(event: Event) {
    event.preventDefault();
    event.stopPropagation();

    if (this.selectedTemplate && this.selectedRepository && this.selectedRef) {
      const templateJSON = JSON.parse(this.selectedTemplate.template);
      const extraConfig = JSON.parse(templateJSON.extraConfig);

      extraConfig.source = this.selectedSource;
      extraConfig.hfToken = this.selectedHfToken;
      if (this.selectedSource === 'hub') {
        extraConfig.branchToDeploy = this.selectedRepository + '/' + this.selectedRef.id;
        extraConfig.branchToDeployDisplayName = extraConfig.modelName.split('/')[0] + '/' + this.selectedRef.id;
        extraConfig.loraSourceModel = this.selectedRef.metadata?.lora_adapter === 'true' ?
          this.selectedRef.metadata?.source_model : null;
      } else {
        extraConfig.branchToDeploy = null;
        extraConfig.loraSourceModel = null;
      }

      templateJSON.extraConfig = JSON.stringify(extraConfig);
      this.selectedTemplate.template = JSON.stringify(templateJSON, null, 2);
      await lastValueFrom(this.appTemplateService.update(this.selectedTemplate));

      await this.createApp(this.selectedTemplate);
    }
  }

  showCreateEditDialog(appTemplate?: IAppTemplate) {
    this.templateForm.reset();
    if (appTemplate) {
      const templateJSON = JSON.parse(appTemplate.template);
      const extraConfig = templateJSON.extraConfig ? JSON.parse(templateJSON.extraConfig) : null;
      templateJSON.extraConfig = null;
      this.templateForm.patchValue({
        ...appTemplate,
        template: JSON.stringify(templateJSON, null, 2),
        extraConfig: extraConfig ? JSON.stringify(extraConfig, null, 2) : null
      });
    }
    this.displayCreateDialog = true;
  }

  hideCreateDialog(): void {
    this.displayCreateDialog = false;
  }

  async saveTemplate() {
    if (this.templateForm.valid) {
      const appTemplate = this.appTemplateFormService.getAppTemplate(this.templateForm);
      const templateJSON = JSON.parse(appTemplate.template);
      templateJSON.extraConfig = appTemplate.extraConfig;
      appTemplate.template = JSON.stringify(templateJSON, null, 2);
      if (appTemplate.id) {
        await lastValueFrom(this.appTemplateService.update(appTemplate));
      } else {
        await lastValueFrom(this.appTemplateService.create(appTemplate));
      }

      await this.initTemplates();
      this.hideCreateDialog();
    }
  }

  confirmDelete(appTemplate: IAppTemplate): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete App Template ${appTemplate.name}?`,
      header: 'Confirm Delete Operation',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        await lastValueFrom(this.appTemplateService.delete(appTemplate.id!));
        await this.initTemplates();
      }
    });
  }

  async cloneTemplate(appTemplate: IAppTemplate) {
    const newTemplate = {...appTemplate} as IAppTemplate;
    newTemplate.name = appTemplate.name+ ' Copy';
    newTemplate.id = null;
    await lastValueFrom(this.appTemplateService.create(newTemplate));
    await this.initTemplates();
  }

  formatJson(field: string) {
    const ctrl = this.templateForm.get(field);
    if (!ctrl?.value) {
      return;
    }
    try {
      ctrl?.setValue(JSON.stringify(JSON.parse(ctrl?.value), null, 2));
    } catch {}
  }


  protected readonly ApplicationMode = ApplicationMode;
  readonly welcomeItems = [
    { title: 'Surogate Concepts', link: 'https://surogate.ai/' },
    { title: 'Applications in Surogate', link: 'https://docs.statemesh.net/applications/intro' },
    { title: 'Create a new Application', link: 'https://docs.statemesh.net/applications/create' }
  ];
  protected readonly Server = Server;
  protected readonly CodeXml = CodeXml;
  protected readonly Rocket = Rocket;
  protected readonly BookOpen = BookOpen;
  protected readonly ContainerType = ContainerType;
}
