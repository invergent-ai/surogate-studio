import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  standalone: true,
  name: 'stripAnsi'
})
export default class StripAnsiPipe implements PipeTransform {
  transform(value: string): string {
    return value?.replace(/\x1B\[[0-?]*[ -/]*[@-~]/g, '') ?? '';
  }
}
