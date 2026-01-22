import { Pipe, PipeTransform } from '@angular/core';

import dayjs from 'dayjs/esm';

@Pipe({
  standalone: true,
  name: 'duration',
})
export default class DurationPipe implements PipeTransform {
  transform(value: any, unit: any): string {
    if (value) {
      return dayjs.duration(value, unit).humanize();
    }
    return '';
  }
}
