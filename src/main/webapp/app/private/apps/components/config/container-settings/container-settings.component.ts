import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { FormArray, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { MessageService, SharedModule } from 'primeng/api';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { DropdownModule } from 'primeng/dropdown';
import { debounceTime, distinctUntilChanged, firstValueFrom, lastValueFrom, Subject } from 'rxjs';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { InputSwitchModule } from 'primeng/inputswitch';
import { ButtonModule } from 'primeng/button';
import { DockerHubSearchComponent } from './registry-search/docker-search/docker-hub-search.component';
import { PortSettingsComponent } from './port-settings/port-settings.component';
import { takeUntil } from 'rxjs/operators';
import { DockerHubImage, DockerHubTag, SelectedDockerImage } from '../../../../../shared/model/docker/docker-hub.model';
import { DockerHubService } from '../../../../../shared/service/docker/docker-hub.service';
import { IPort } from '../../../../../shared/model/port.model';
import { VolumeComponent } from './volume/volume.component';
import { AdvancedSettingsComponent } from './advanced-settings/advanced-settings.component';
import { DockerPortInfoModelPortInfo } from '../../../../../shared/model/docker/docker-port-info.model';
import { RegistrySettingsComponent } from './registry-settings/registry-settings.component';
import { AccordionModule } from 'primeng/accordion';
import { Account } from '../../../../../shared/model/account.model';
import { AccountService } from '../../../../../shared/service/account.service';
import { ApplicationMode } from '../../../../../shared/model/enum/application-mode.model';
import { CardModule } from 'primeng/card';

@Component({
  selector: 'sm-container-settings',
  templateUrl: './container-settings.component.html',
  styleUrls: ['./container-settings.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    TooltipModule,
    AutoCompleteModule,
    InputSwitchModule,
    FormsModule,
    ButtonModule,
    DockerHubSearchComponent,
    PortSettingsComponent,
    VolumeComponent,
    AdvancedSettingsComponent,
    RegistrySettingsComponent,
    AccordionModule,
    NgOptimizedImage,
    CardModule
  ]
})
export class ContainerSettingsComponent implements OnInit, OnChanges, OnDestroy {
  @Input() containerForm!: FormGroup;
  @Input() applicationForm!: FormGroup;
  @Input() applicationName!: string;
  @Input() publicIp: string;
  @Input() applicationId!: string;
  @Input() containerIndex: number = 0;
  @Input() namespace: string = undefined;
  @Input() resetTrigger!: Subject<void>;
  @Input() hasExistingIngressPort?: boolean;
  @Input() mode: keyof typeof ApplicationMode = ApplicationMode.APPLICATION;
  @Output() portsUpdated = new EventEmitter<{ ports: IPort[]; containerIndex: number }>();
  @ViewChild(VolumeComponent) volumeComponent?: VolumeComponent;

  private destroy$ = new Subject<void>();
  registries: any[] = [
    {label: 'Docker Hub', value: 'docker'}
  ];
  availableTags: {label: string, value: string}[] = [
    {label: 'empty', value: 'empty'}
  ];
  selectedRegistry: any = this.registries[0];
  selectedTag: string = 'latest';
  selectedImage: SelectedDockerImage | null = null;

  defaultPorts: DockerPortInfoModelPortInfo[];
  defaultVolumes: string[] = [];
  ports: IPort[] = [];
  user: Account;

  isPrivateRegistry: boolean = false;
  isDockerHubImage: boolean = false;
  showRegistrySettings = false;
  showRegistrySearch = false;

  protected registryCredentials?: {
    registryUrl: string;
    registryUser: string;
    registryPassword: string;
  };

  constructor(
    private dockerHubService: DockerHubService,
    private accountService: AccountService,
    private messageService: MessageService
  ) {}

  async ngOnInit() {
    this.user = await lastValueFrom(this.accountService.identity());
    this.initializeForm();
    await this.restoreContainerState();
    this.setupPortsControl();
  }

  toggleRegistrySearch(): void {
    if (this.isPrivateRegistry && !this.containerForm.get('registryPassword')?.value) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Warning',
        detail: 'Please reenter your registry password.'
      });
      this.showRegistrySettings = true;
      return;
    }

    this.showRegistrySearch = !this.showRegistrySearch;
    this.showRegistrySettings = false;
  }

  toggleRegistrySettings(): void {
    this.showRegistrySettings = !this.showRegistrySettings;
    if (this.showRegistrySettings == true) {
      this.showRegistrySearch = false
    }
  }

  switchToDockerHub(): void {
    this.selectedRegistry = this.registries[0];
    this.isPrivateRegistry = false;
    this.showRegistrySearch = true;
    this.showRegistrySettings = false;
  }

  switchToPrivateRegistry() {
    this.isPrivateRegistry = true;
    const registry = {
      registryUrl: this.containerForm.get('registryUrl')?.value,
      registryUser: this.containerForm.get('registryUsername')?.value,
      applicationId: this.applicationId,
      imageName: this.containerForm.get('imageName')?.value
    };
    this.registries = [...this.registries, {
      label: registry.registryUrl,
      value: registry
    }];
    this.selectedRegistry = this.registries[this.registries.length - 1];
  }

  onRegistryValidated(registry: any): void {
    this.registries = [...this.registries, {
      label: registry.registryUrl,
      value: registry
    }];
    this.containerForm.get("registryUrl").setValue(registry.registryUrl);
    this.containerForm.get("registryUsername").setValue(registry.registryUser);
    this.containerForm.get("registryPassword").setValue(registry.registryPassword);

    this.selectedRegistry = this.registries[this.registries.length - 1];
    this.isPrivateRegistry = true;
    this.registryCredentials = registry;
    this.showRegistrySettings = false;
    this.showRegistrySearch = true;
  }

  onCancelPrivateRegistry() {
    this.switchToDockerHub();
    this.containerForm.get('registryUrl').setValue(null);
    this.containerForm.get('registryUsername').setValue(null);
    this.containerForm.get('registryPassword').setValue(null);
  }

  onDockerImageSelected(dockerImage: DockerHubImage): void {
    // Clear existing ports and volumes
    this.defaultPorts = [];
    this.defaultVolumes = [];
    const portsArray = this.containerForm.get('ports') as FormArray;
    const volumeMountsArray = this.containerForm.get('volumeMounts') as FormArray;
    portsArray.clear();
    volumeMountsArray.clear();

    this.selectedImage = {
      ...dockerImage,
      tag: dockerImage.tags?.[0] || 'latest'
    };

    if (this.selectedRegistry?.value === 'docker') {
      this.handlePublicDockerImageSelection(dockerImage);
      this.loadImageTags(dockerImage.name);
    } else {
      this.handlePrivateDockerImageSelection(dockerImage);
    }
    this.showRegistrySearch = false;
  }

  private handlePublicDockerImageSelection(image: DockerHubImage): void {
    const defaultTag = image.tags?.[0] || 'latest';
    const [namespace, imageName] = image.name.includes('/') ?
      image.name.split('/') :
      [null, image.name];

    this.containerForm.patchValue({
      imageName: image.name,
      imageTag: defaultTag
    });

    this.dockerHubService.getImagePortsAndVolumes(namespace, imageName, defaultTag)
      .subscribe({
        next: (config) => {
          this.defaultPorts = config.ports;
          this.defaultVolumes = config.volumes;
          this.volumeComponent?.useDefaultVolumes();
        },
        error: () => {
          this.defaultPorts = [];
          this.defaultVolumes = [];
        }
      });
  }

  private handlePrivateDockerImageSelection(image: DockerHubImage): void {
    const defaultTag = image.tags?.[0] || 'latest';

    // If no '/', use image name as both namespace and imageName
    const [namespace, imageName] = image.name.includes('/') ?
      image.name.split('/') :
      [image.name, image.name];

    this.containerForm.patchValue({
      imageName: image.name,
      imageTag: defaultTag
    });

    this.availableTags = (image.tags || []).map(tag => ({
      label: tag, value: tag
    }));

    this.getPrivateRegistryImageConfig(namespace, imageName, defaultTag);
  }


  onPortsChange(ports: IPort[]): void {
    this.ports = ports.map(port => ({
      ...port,
      containerIndex: this.containerIndex
    }));

    this.portsUpdated.emit({
      ports: this.ports,
      containerIndex: this.containerIndex
    });
  }

  private initializeForm(): void {
    if (!this.containerForm.contains('imageTag')) {
      this.containerForm.addControl('imageTag', new FormControl('latest'));
    }

    this.containerForm.get('imageName')?.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (!value) {
        this.isDockerHubImage = false;
        this.selectedImage = null;
      }
    });
  }

  private setupPortsControl(): void {
    if (!this.containerForm.contains('ports')) {
      this.containerForm.addControl('ports', new FormArray([]));
    }

    const existingPorts = this.containerForm.get('ports')?.value;
    if (existingPorts?.length) {
      this.ports = existingPorts;
    }
  }

  private async restoreContainerState(): Promise<void> {
    const imageName = this.containerForm.get('imageName')?.value;
    const imageTag = this.containerForm.get('imageTag')?.value;
    if (this.containerForm.get('registryUrl')?.value) {
      this.switchToPrivateRegistry();
    } else {
      this.switchToDockerHub();
      this.showRegistrySearch = false;
    }

    if (imageName) {
      // Set initial tag state
      this.availableTags = [{label: imageTag, value: imageTag}];
      this.selectedTag = imageTag;

      this.selectedImage = {
        name: imageName,
        tag: imageTag,
        description: '',
        stars: 0,
        official: false,
        automated: false,
        logo_url: '',
        tags: [imageTag]
      };

      this.containerForm.get('imageTag')?.setValue(imageTag, {emitEvent: false});
      this.availableTags = [{label: imageTag, value: imageTag}];  // Initialize with current tag
      this.selectedTag = imageTag;

      // Load additional tags if needed
      if (this.registryCredentials) {
        await this.loadPrivateRegistryTags(imageName);
      } else {
        if (this.isPrivateRegistry && !this.containerForm.get('registryPassword')?.value) {
          return;
        }

        this.loadImageTags(imageName);
      }
    }
  }

  private async loadPrivateRegistryTags(imageName: string) {
    if (!this.registryCredentials) return;

    try {
      const response = await firstValueFrom(
        this.dockerHubService.searchCustomRegistry({
          ...this.registryCredentials,
          searchTerm: imageName
        })
      );

      const image = response.results.find(img => img.name === imageName);
      if (image) {
        this.availableTags = image.tags?.map(tag => ({
          label: tag,
          value: tag
        })) || [{label: 'latest', value: 'latest'}];
      }
    } catch (error) {
      console.error('Failed to load private registry tags:', error);
      this.availableTags = [{label: 'latest', value: 'latest'}];
    }
  }

  private getPrivateRegistryImageConfig(namespace: string, imageName: string, tag: string): void {

    if (!this.selectedRegistry?.value || !this.registryCredentials) {
      return;
    }

    const params = {
      registryUrl: this.selectedRegistry.value.registryUrl,
      registryUser: this.registryCredentials.registryUser,
      registryPassword: this.registryCredentials.registryPassword,
      namespace,
      imageName,
      tag
    };


    this.dockerHubService.getRegistryImagePortsAndVolumes(params)
      .subscribe({
        next: (config: {
          ports: DockerPortInfoModelPortInfo[];
          volumes: string[];
        }) => {
          this.defaultPorts = config.ports;
          this.defaultVolumes = config.volumes;
          this.volumeComponent?.useDefaultVolumes();
        },
        error: (err) => {
          console.error('Error getting registry image config:', err);
          this.defaultPorts = [];
          this.defaultVolumes = [];
        }
      });
  }

  loadImageTags(imageName: string): void {
    const currentTag = this.containerForm.get('imageTag')?.value;
    const [namespace, imageNameOnly] = imageName.includes('/') ?
      imageName.split('/') :
      [null, imageName];

    const service = this.selectedRegistry?.value === 'docker' ?
      this.dockerHubService.getImageTags(imageName) :
      this.dockerHubService.getRegistryImageTags(this.selectedRegistry.value, imageName);

    service.subscribe({
      next: (response) => {
        const newTags = response.results.map((tag: DockerHubTag) => ({
          label: tag.name,
          value: tag.name
        }));

        if (currentTag && !newTags.find(t => t.value === currentTag)) {
          newTags.unshift({label: currentTag, value: currentTag});
        }

        this.availableTags = newTags;

        if (!currentTag) {
          const firstTag = this.availableTags[0]?.value || 'latest';
          this.selectedTag = firstTag;
          this.containerForm.patchValue({imageTag: firstTag});
        } else {
          this.selectedTag = currentTag;
        }

        this.dockerHubService.getImagePortsAndVolumes(namespace, imageNameOnly, this.selectedTag)
          .subscribe({
            next: (config) => {
              this.defaultPorts = config.ports;
              this.defaultVolumes = config.volumes;
            },
            error: () => this.defaultPorts = []
          });
      },
      error: () => {
        if (!currentTag) {
          this.availableTags = [{label: 'latest', value: 'latest'}];
          this.containerForm.patchValue({imageTag: 'latest'});
        } else {
          this.availableTags = [{label: currentTag, value: currentTag}];
        }
        this.defaultPorts = [];
      }
    });
  }

  onTagChange(_: any) {
    this.showRegistrySettings = false;
    this.containerForm.get('imageTag').setValue(this.containerForm.get('imageTag').value);
  }

  async ngOnChanges(changes: SimpleChanges) {
    if (changes['containerForm'] && !changes['containerForm'].firstChange) {
      await this.restoreContainerState();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected readonly ApplicationMode = ApplicationMode;
}
