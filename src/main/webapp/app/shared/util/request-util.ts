import { HttpParams } from '@angular/common/http';

export const createRequestOption = (req?: any): HttpParams => {
  let options: HttpParams = new HttpParams();

  if (req) {
    Object.keys(req).forEach(key => {
      if (key !== 'sort' && key !== 'criteria' && req[key] !== undefined) {
        for (const value of [].concat(req[key]).filter(v => v !== '')) {
          options = options.append(key, value);
        }
      }
    });

    if (req.sort) {
      req.sort.forEach((val: string) => {
        options = options.append('sort', val);
      });
    }

    if (req.criteria) {
      req.criteria.forEach((criterion: { key: string; value: string | number | boolean }) => {
        options = options.append(criterion.key, criterion.value);
      });
    }
  }

  return options;
};
