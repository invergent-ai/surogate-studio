import {AfterViewInit, Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from "@angular/core";
import {FormControl, ReactiveFormsModule} from "@angular/forms";
import {ChipsModule} from "primeng/chips";
import {CommonModule} from "@angular/common";
import {DockerHubImage} from '../../../../../../../shared/model/docker/docker-hub.model';
import {DockerHubService} from '../../../../../../../shared/service/docker/docker-hub.service';
import {PageLoadComponent} from '../../../../../../../shared/components/page-load/page-load.component';
import SharedModule from '../../../../../../../shared/shared.module';
import {firstValueFrom} from "rxjs";
import {ScrollerModule} from "primeng/scroller";

@Component({
  selector: 'sm-docker-hub-search',
  templateUrl: './docker-hub-search.component.html',
  styleUrls: ['./docker-hub-search.component.scss'],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    ChipsModule,
    CommonModule,
    PageLoadComponent,
    SharedModule,
    ScrollerModule
  ]
})
export class DockerHubSearchComponent implements OnInit, AfterViewInit {
  @Input() registry: any;
  @Output() imageSelected = new EventEmitter<DockerHubImage>();
  @Output() registryChanged = new EventEmitter<void>();
  @ViewChild("searchInput") searchInput: ElementRef;

  private imageTagsMap = new Map<string, string[]>();
  searchControl: FormControl;
  images: DockerHubImage[] = [];
  isLoading = false;

  constructor(private dockerHubService: DockerHubService) {
    this.searchControl = new FormControl('');
  }

  async ngOnInit(): Promise<void> {
    await this.loadImages();
  }

  ngAfterViewInit() {
    if (this.searchInput) {
      this.searchInput.nativeElement.focus();
    }
  }

  async loadImages(): Promise<void> {
    if (!this.registry?.value || this.registry.value === 'docker') {
      await this.loadPopularImages();
    } else {
      await this.loadCustomRegistryImages();
    }
  }

  switchToDockerHub(): void {
    this.registry = {label: 'Docker Hub', value: 'docker'};
    this.registryChanged.emit();
    this.searchControl.setValue(''); // Reset search
    this.dockerHubService.searchImages('').subscribe({
      next: (response) => {
        this.images = response.results;
      },
      error: (error) => console.error('Failed to load images:', error)
    });
  }

  private async loadCustomRegistryImages(): Promise<void> {
    try {
      this.isLoading = true;
      const response = await firstValueFrom(
        this.dockerHubService.searchCustomRegistry(this.registry.value)
      );
      // Store images and their tags
      this.images = response.results;
      // Create map of image names to their tags
      this.imageTagsMap.clear();
      response.results.forEach(img => {
        this.imageTagsMap.set(img.name, img.tags || ['latest']);
      });
    } catch (error) {
      console.error('Search failed:', error);
    } finally {
      this.isLoading = false;
    }
  }

  async loadPopularImages(): Promise<void> {
    try {
      this.isLoading = true;
      const response = await firstValueFrom(this.dockerHubService.searchImages(''));
      this.images = response.results;
    } catch (error) {
      console.error('Failed to load popular images:', error);
    } finally {
      this.isLoading = false;
    }
  }

  async onSearch(_: KeyboardEvent): Promise<void> {
    const searchValue = this.searchControl.value?.trim();
    if (!searchValue) {
      await this.loadImages();
      return;
    }

    try {
      this.isLoading = true;
      if (!this.registry?.value || this.registry.value === 'docker') {
        const response = await firstValueFrom(this.dockerHubService.searchImages(searchValue));
        this.images = response.results;
      } else {
        const response = await firstValueFrom(
          this.dockerHubService.searchCustomRegistry({
            ...this.registry.value,
            searchTerm: searchValue
          })
        );
        this.images = response.results;
      }
    } catch (error) {
      console.error('Search failed:', error);
    } finally {
      this.isLoading = false;
    }
  }

  selectImage(image: DockerHubImage) {
    if (!this.registry?.value || this.registry.value === 'docker') {
      // For public images, load tags separately
      this.loadPublicImageTags(image);
    } else {
      // For private images, use stored tags
      const tags = this.imageTagsMap.get(image.name) || ['latest'];
      const privateImage: DockerHubImage = {
        ...image,
        name: image.name,
        tags
      };
      this.imageSelected.emit(privateImage);
    }
  }

  private loadPublicImageTags(image: DockerHubImage) {
    this.isLoading = true;
    this.dockerHubService.getImageTags(image.name).subscribe({
      next: (response) => {
        const tags = response.results.map((tag: any) => tag.name);
        const publicImage: DockerHubImage = {
          ...image,
          tags: tags.length > 0 ? tags : ['latest']
        };
        this.imageSelected.emit(publicImage);
      },
      error: (error) => {
        console.error('Failed to load image tags:', error);
        const publicImage: DockerHubImage = {
          ...image,
          tags: ['latest']
        };
        this.imageSelected.emit(publicImage);
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }
}
