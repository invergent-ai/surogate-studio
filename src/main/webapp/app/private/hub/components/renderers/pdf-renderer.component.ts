import {Component, effect, inject, input, OnInit, signal} from '@angular/core';
import {DomSanitizer, SafeResourceUrl, SafeUrl} from "@angular/platform-browser";
import {DirectLakeFsService} from "../../../../shared/service/direct-lake-fs.service";
import {NgIf} from "@angular/common";

@Component({
  selector: 'sm-pdf-renderer',
  standalone: true,
  imports: [
    NgIf
  ],
  template: `
    <div class="object-viewer-pdf">
      <ng-container *ngIf="pdfUrl(); else loading">
        <object [data]="pdfUrl()" type="application/pdf" width="100%" height="600px"></object>
      </ng-container>
      <ng-template #loading>
        <div class="spinner">Loading PDF...</div>
      </ng-template>
    </div>
  `
})
export class PdfRendererComponent {
  repoId = input.required<string>();
  refId = input.required<string>();
  path = input.required<string>();

  private sanitizer = inject(DomSanitizer);
  private directLakeFsService = inject(DirectLakeFsService);

  private pdfUrlSignal = signal<SafeResourceUrl | null>(null);
  pdfUrl = this.pdfUrlSignal.asReadonly();

  private loadPdf = effect(() => {
    const repo = this.repoId();
    const ref = this.refId();
    const filePath = this.path();

    if (!repo || !ref || !filePath) return;

    this.directLakeFsService.download(repo, ref, filePath).subscribe({
      next: (response) => {
        const blob = response.body;
        if (blob) {
          const objectUrl = URL.createObjectURL(blob);
          this.pdfUrlSignal.set(this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl));
        }
      },
      error: (err) => {
        console.error('❌ Eroare la încărcarea PDF-ului', err);
      }
    });
  });
}
