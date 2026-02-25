import { Component, EventEmitter, Output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { firstValueFrom } from 'rxjs';
import { GhcrService } from '../../../../../../../shared/service/ghcr/ghcr.service';
import { DockerHubImage } from '../../../../../../../shared/model/docker/docker-hub.model';

@Component({
  selector: 'sm-ghcr-search',
  templateUrl: './ghcr-search.component.html',
  styleUrls: ['./ghcr-search.component.scss'],
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, InputTextModule, ButtonModule],
})
export class GhcrSearchComponent {
  @Output() imageSelected = new EventEmitter<{ image: DockerHubImage; owner: string }>();
  @Output() cancel = new EventEmitter<void>();

  repoControl = new FormControl('');
  isLoading = false;
  errorMessage: string | null = null;

  constructor(private ghcrService: GhcrService) {}

  async submit(): Promise<void> {
    let value = this.repoControl.value?.trim();
    if (!value) return;

    // Strip ghcr.io/ prefix if user included it
    if (value.toLowerCase().startsWith('ghcr.io/')) {
      value = value.substring('ghcr.io/'.length);
    }

    if (!value.includes('/')) {
      this.errorMessage = 'Please use the format: owner/image-name';
      return;
    }

    const slashIndex = value.indexOf('/');
    const owner = value.substring(0, slashIndex);
    const imageName = value.substring(slashIndex + 1);

    if (!owner || !imageName) return;

    try {
      this.isLoading = true;
      this.errorMessage = null;

      const response = await firstValueFrom(this.ghcrService.getTags(owner, imageName));
      const tags = response.results.map(t => t.name);

      const image: DockerHubImage = {
        name: `${owner}/${imageName}`,
        description: '',
        stars: 0,
        official: false,
        automated: false,
        logo_url: null,
        tags: tags.length > 0 ? tags : ['latest'],
      };

      this.imageSelected.emit({ image, owner });
    } catch (error) {
      this.errorMessage = 'Failed to load tags. Check the repository name.';
    } finally {
      this.isLoading = false;
    }
  }
}
