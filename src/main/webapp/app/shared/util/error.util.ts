import {HttpErrorResponse} from '@angular/common/http';
import {Message} from 'primeng/api';
import {IApplication} from "../model/application.model";
import {APP_ERROR_BUILD_CICD_KEY} from "../../config/constant/error.constants";
import {ApplicationStatus} from "../model/enum/application-status.model";
import {Store} from '@ngxs/store';
import {DisplayGlobalMessageAction} from '../state/actions';

declare class IProblemDetailWithCause {
  cause?: IProblemDetailWithCause;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  properties?: Record<string, any>;
}

// Define a map for error codes and their corresponding messages
const errorMessages = new Map<string, string>([
  ['EMAIL_SENDING_PROBLEM', 'There was a problem sending the email'],
  ['INVALID_ACTIVATION_CODE', 'The activation code is invalid'],
  ['NOT_AUTHORIZED', 'You are not authorized to perform this action'],
  ['OBJECT_NOT_FOUND', 'The requested object could not be found'],
  ['OBJECT_EXISTS', 'The object already exists'],
  ['USER_ALREADY_EXISTS', 'This email address is already registered'],
  ['USER_NOT_FOUND', 'The user could not be found'],
  ['error.http.500', 'Internal error'],
  ['error.validation', 'Validation error'],
  ['error.userexists', 'There is already an account with the provided e-mail address'],
  ['error.not.activated', 'Please activate your account'],
  ['error.project.hasApplications', 'The project could not be deleted. Remove the applications inside first.'],
  ['error.bad.credentials', 'Invalid username or password'] // TODO - NTH - Use translationService
]);

// Function to display error message using a dialog
export const displayError = (store: Store, error: any) => {
  const message = getErrorMessage(error);
  store.dispatch(new DisplayGlobalMessageAction({
    severity: 'error',
    summary: 'Error',
    detail: message,
    closable: true,
    life: 6000,
  } as Message));
};

export const displayErrorAndRethrow = (store: Store, e: Error) => {
  displayError(store, e);
  throw e;
}

// Function to get error message based on the error object
export const getErrorMessage = (error: any): string => {
  if (error instanceof HttpErrorResponse) {
    if (error.error?.status && error.error?.type) {
      const problem = error.error as IProblemDetailWithCause;
      if (problem.detail) {
        return problem.detail;
      }
      if (problem.title) {
        return problem.title;
      }
    }

    if (error.error?.message) {
      const errorMsg = errorMessages.get(error.error?.message);
      if (errorMsg) return errorMsg;
    } else if (error.error?.detail) {
      return error.error.detail;
    }

    if (error.error?.messages) {
      return (error.error.messages as Array<string>).join('<br/>');
    }
    if (error.statusText) {
      return error.statusText;
    }
  }

  return error;
};

export const ciCdError = (application?: IApplication): boolean => {
  return application?.status === ApplicationStatus.ERROR && application?.errorMessageKey === APP_ERROR_BUILD_CICD_KEY;
}
