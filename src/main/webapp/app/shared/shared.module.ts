import { NgModule, SecurityContext } from '@angular/core';
import {CommonModule, NgForOf, NgIf} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';

import FindLanguageFromKeyPipe from './language/find-language-from-key.pipe';
import TranslateDirective from './language/translate.directive';
import {RippleModule} from "primeng/ripple";
import {ButtonModule} from "primeng/button";
import {DialogModule} from "primeng/dialog";
import {ToastModule} from "primeng/toast";
import {DialogService} from "primeng/dynamicdialog";
import {MessageService} from "primeng/api";
import {InputTextModule} from "primeng/inputtext";
import {DividerModule} from "primeng/divider";
import {PasswordModule} from "primeng/password";
import {CheckboxModule} from "primeng/checkbox";
import {DropdownModule} from "primeng/dropdown";
import {KeyFilterModule} from "primeng/keyfilter";
import {StyleClassModule} from "primeng/styleclass";
import {TableModule} from "primeng/table";
import {RatingModule} from "primeng/rating";
import {FileUploadModule} from "primeng/fileupload";
import {ToolbarModule} from "primeng/toolbar";
import {InputNumberModule} from "primeng/inputnumber";
import {RadioButtonModule} from "primeng/radiobutton";
import {InputTextareaModule} from "primeng/inputtextarea";
import {FormsModule} from "@angular/forms";
import { MarkdownModule, MARKED_OPTIONS, MarkedOptions, MarkedRenderer } from 'ngx-markdown';
import { LucideAngularModule } from 'lucide-angular';

export function markedOptionsFactory(): MarkedOptions {
  const renderer = new MarkedRenderer();

  renderer.image = (href: string, title: string, text: string): string => {
    return '<img src="' + href + '" alt="' + text + '" title="' + (title || '') + '" class="w-full" />';
  };

  renderer.link = (href: string, title: string, text: string): string => {
    if (href && href.endsWith(".mp4")) {
      return '<video src="' + href + '" controls="controls" muted="muted" class="block w-fit"></video>';
    }
    return `<a href="${href}" target="_blank" rel="noopener noreferrer" class="text-primary-700 hover:text-primary-600">${text}</a>`;
  }

  return {
    renderer: renderer,
    gfm: true,
    breaks: false,
    pedantic: false,
    silent: true
  };
}

/**
 * Application wide Module
 */
@NgModule({
  imports: [
    CommonModule,
    TranslateModule,
    FindLanguageFromKeyPipe,
    TranslateDirective,
    RippleModule,
    NgForOf,
    NgIf,
    ButtonModule,
    DialogModule,
    ToastModule,
    DividerModule,
    PasswordModule,
    CheckboxModule,
    InputTextModule,
    DropdownModule,
    KeyFilterModule,
    StyleClassModule,
    TableModule,
    RatingModule,
    ToolbarModule,
    FileUploadModule,
    FormsModule,
    InputTextareaModule,
    RadioButtonModule,
    InputNumberModule,
    MarkdownModule.forRoot({
      sanitize: SecurityContext.NONE,
      markedOptions: {
        provide: MARKED_OPTIONS,
        useFactory: markedOptionsFactory
      }
    }),
    LucideAngularModule
  ],
  providers: [
    DialogService,
    MessageService
  ],
  exports: [
    CommonModule,
    TranslateModule,
    FindLanguageFromKeyPipe,
    TranslateDirective,
    RippleModule,
    NgForOf,
    NgIf,
    ButtonModule,
    DialogModule,
    ToastModule,
    DividerModule,
    PasswordModule,
    CheckboxModule,
    InputTextModule,
    DropdownModule,
    KeyFilterModule,
    StyleClassModule,
    TableModule,
    RatingModule,
    ToolbarModule,
    FileUploadModule,
    FormsModule,
    InputTextareaModule,
    RadioButtonModule,
    InputNumberModule,
    LucideAngularModule
  ]
})
export default class SharedModule {}
