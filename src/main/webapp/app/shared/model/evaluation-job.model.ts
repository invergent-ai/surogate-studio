import {RefSelection} from "../../private/hub/components/ref-selector.component";

export interface IEvaluationJob {
  id?: number;
  runName?: string;
  baseModel?: string;
  baseModelRef?: RefSelection;
  description?: string;
  language?: string;
  judgeModel?: string;
  judgeModelApi?: string;
  useGateway?: boolean;
  status?: 'DRAFT' | 'ARCHIVED';
  notify?: string[];
  benchmarks?: IEvaluationBenchmark[];
}

export interface IEvaluationBenchmark {
  id?: number;
  name?: string;
  type?: string;
  shots?: number;
  selectedTasks?: string[];
}
