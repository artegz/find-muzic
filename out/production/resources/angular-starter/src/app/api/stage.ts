import {Step} from "./step";
import {ProgressBar} from "./progress-bar";

export class Stage {

  name: string;
  description: string;
  stageSteps: Step[];
  inProgressStageSteps: Step[];
  completeStageSteps: Step[];
  subStage: Stage;
  progressBar: ProgressBar;

}
