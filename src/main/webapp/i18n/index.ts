/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck

const angularLanguages = {
  en: async (): Promise<void> => import('@angular/common/locales/en'),
};

const languagesData = {
  en: async (): Promise<any> => import('i18n/default/en.json'),
};

export const loadLocale = (locale: keyof typeof angularLanguages): Promise<any> => {
  angularLanguages[locale]();
  const langData = languagesData[locale]();
  // Merge default language data with profile language data
  return Promise.all([langData]).then(([lang]) => {
    const result = {...lang};
    delete result['default'];
    return result;
  });
};
