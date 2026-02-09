import { Component, inject, input, OnInit, signal } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { LakeFsService } from '../../../../shared/service/lake-fs.service';

@Component({
  selector: 'sm-image-renderer',
  standalone: true,
  template: `
    <p class="image-container">
      <img [src]="imageUrl()" [alt]="path()" />
    </p>
  `,
})
export class ImageRendererComponent implements OnInit {
  repoId = input.required<string>();
  refId = input.required<string>();
  path = input.required<string>();

  private sanitizer = inject(DomSanitizer);
  private lakeFsService = inject(LakeFsService);

  private imageUrlSignal = signal<SafeUrl | null>(null);
  imageUrl = this.imageUrlSignal.asReadonly();

  ngOnInit() {
    this.lakeFsService.download(this.repoId(), this.refId(), this.path()).subscribe({
      next: response => {
        const blob = response.body;
        if (blob) {
          const objectUrl = URL.createObjectURL(blob);
          this.imageUrlSignal.set(this.sanitizer.bypassSecurityTrustUrl(objectUrl));
        }
      },
      error: err => {
        console.error('❌ Eroare la încărcarea imaginii', err);
      },
    });
  }
}
