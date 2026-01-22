import {IApplication} from "./application.model";

export interface IAnnotation {
  id: string;
  key?: string | null;
  value?: string | null;
  application?: Pick<IApplication, 'id'> | null;
}

export type NewAnnotation = Omit<IAnnotation, 'id'> & { id: null };
