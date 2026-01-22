import { Store } from '@ngxs/store';
import { DisplayGlobalMessageAction } from '../state/actions';
import { Message } from 'primeng/api';

export const displaySuccess = (store: Store, message: string, life = 6000) => {
  store.dispatch(new DisplayGlobalMessageAction({
    severity: 'success',
    summary: 'Success',
    icon: 'pi pi-info-circle',
    detail: message,
    life,
    closable: true
  } as Message));
};

export const displayWarning = (store: Store, message: string) => {
  store.dispatch(new DisplayGlobalMessageAction({
    severity: 'warn',
    summary: 'Warning',
    icon: 'pi pi-exclamation-triangle',
    detail: message,
    life: 60000,
    closable: true
  } as Message));
};
