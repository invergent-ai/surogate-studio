// app-template.model.ts
export interface ProviderDTO {
  id: string;
  name: string;
  description?: string;
  active?: boolean;
}

export interface IAppTemplate {
  id: string;
  name?: string | null;
  description?: string | null;
  longDescription?: string | null;
  icon?: string | null;
  template?: string | null;
  category?: string | null;
  zorder?: number | null;
  provider?: ProviderDTO | null;
  hashtags?: string | null;
}

export type AppTemplateExtra = IAppTemplate & { extraConfig: string; };
export type NewAppTemplate = Omit<AppTemplateExtra, 'id'> & { id: null };
