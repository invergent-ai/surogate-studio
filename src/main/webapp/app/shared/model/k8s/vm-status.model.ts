import { VirtualMachineStatus } from '../enum/vm-status.model';

export interface VmStatus {
  stage: VirtualMachineStatus;
  message: string;
  type?: string;
  error?: string;
  // Transient
  vmId?: string;
}
