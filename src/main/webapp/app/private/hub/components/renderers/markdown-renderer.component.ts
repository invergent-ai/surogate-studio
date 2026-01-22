import {Component, computed, inject, input, OnInit, signal, ViewEncapsulation} from '@angular/core';
import {MarkdownComponent} from "ngx-markdown";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {DirectLakeFsService} from "../../../../shared/service/direct-lake-fs.service";

@Component({
  selector: "sm-markdown-renderer",
  standalone: true,
  imports: [
    MarkdownComponent
  ],
  template: `
    <div markdown [data]="processedText()"></div>`,
  styles: ['img {max-width: 600px;display: block; margin-top:10px; margin-bottom:10px}'],
  encapsulation: ViewEncapsulation.None
})
export class MarkdownRendererComponent {
  repoId = input.required<string>();
  refId = input.required<string>();
  text = input.required<string>();

  private directLakeFsService = inject(DirectLakeFsService);
  private imageUrlMap = signal<Record<string, string>>({});

  processedText = computed(() => {
    let md = this.text() ?? '';

    md = md.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_m, alt, url) => {
      if (!url.startsWith('http')) {
        const cached = this.imageUrlMap()[url];
        if (!cached) {

          this.directLakeFsService
            .download(this.repoId(), this.refId(), url)
            .subscribe({
              next: (resp) => {
                const objectUrl = URL.createObjectURL(resp.body);
                this.imageUrlMap.update((map: Record<string, string>) => ({
                  ...map,
                  [url]: objectUrl,
                }));
              },
              error: (err) => {
                console.error('❌ Eroare la download imagine:', url, err);
              }

            });

          return `![${alt}](#)`;
        }
        return `![${alt}](${cached as string})`;
      }
      return `![${alt}](${url})`;
    });

    return md;
  });
}
