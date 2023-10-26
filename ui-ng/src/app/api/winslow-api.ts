import {FileInfoAttribute, humanReadableFileSize} from "./files-api.service";

/*
 * Represents the properties of a class, excluding its methods.
 *
 * This type transforms a class type `Class` into a type that includes only its member properties but not its methods.
 * Suitable for objects received via the API that have not yet been transformed into class instances.
 *
 * @typeparam Class - The class type from which to extract properties.
 */
export type Raw<Class> = {
  [Key in keyof Class as Class[Key] extends Function ? never : Key]: Class[Key]
}

export class Link {
  name: string;
  role: Role;

  constructor(data: Link) {
    this.name = data.name;
    this.role = data.role;
  }
}

export class UserInfo {
  name: string;
  displayName?: string;
  email?: string;
  active: boolean;
  password?: string[];

  constructor(data: UserInfo) {
    this.name = data.name;
    this.displayName = data.displayName;
    this.email = data.email;
    this.active = data.active;
    this.password = data.password;
  }
}

export class FileInfo {
  name: string;
  directory: boolean;
  path: string;
  fileSize?: number;
  attributes: Record<string, any>;
  fileSizeHumanReadableCached?: string;

  constructor(data: Raw<FileInfo>) {
    this.name = data.name;
    this.directory = data.directory;
    this.path = data.path;
    this.fileSize = data.fileSize;
    this.attributes = data.attributes;
  }

  getAttribute(key: FileInfoAttribute): any {
    return this.attributes != null ? this.attributes[key] : null;
  }

  getFileSizeHumanReadable(): string | undefined {
    if (this.fileSizeHumanReadableCached == undefined) {
      this.fileSizeHumanReadableCached = humanReadableFileSize(this.fileSize);
    }
    return this.fileSizeHumanReadableCached;
  }

  hasAttribute(key: FileInfoAttribute): boolean {
    return this.attributes != null && this.attributes[key] != null;
  }

  isGitRepository(): boolean {
    return this.hasAttribute('git-branch');
  };

  getGitBranch(): string | undefined {
    const attr = this.getAttribute('git-branch');
    if (typeof attr === typeof '') {
      return attr as string;
    } else {
      return undefined;
    }
  };

  setGitBranch(branch: string): void {
    if (this.attributes == null) {
      this.attributes = new Map<string, unknown>();
    }
    this.attributes['git-branch'] = branch;
  };
}

export class AllocInfo {
  title: string;
  cpu: number;
  memory: number;
  gpu: number;

  constructor(data: Raw<AllocInfo>) {
    this.title = data.title;
    this.cpu = data.cpu;
    this.memory = data.memory;
    this.gpu = data.gpu;
  }
}

export class BuildInfo {
  date: string;
  commitHashShort: string;
  commitHashLong: string;

  constructor(data: Raw<BuildInfo>) {
    this.date = data.date;
    this.commitHashShort = data.commitHashShort;
    this.commitHashLong = data.commitHashLong;
  }
}

export class CpuInfo {
  modelName: string;
  utilization: number[];

  constructor(data: Raw<CpuInfo>) {
    this.modelName = data.modelName;
    this.utilization = data.utilization;
  }
}

export class CpuUtilization {
  cpus: number[];

  constructor(data: Raw<CpuUtilization>) {
    this.cpus = data.cpus;
  }
}

export class DiskInfo {
  reading: number;
  writing: number;
  free: number;
  used: number;

  constructor(data: Raw<DiskInfo>) {
    this.reading = data.reading;
    this.writing = data.writing;
    this.free = data.free;
    this.used = data.used;
  }
}

export class GpuInfo {
  id: string;
  vendor: string;
  name: string;
  computeUtilization: number;
  memoryUtilization: number;
  memoryUsedMegabytes: number;
  memoryTotalMegabytes: number;

  constructor(data: Raw<GpuInfo>) {
    this.id = data.id;
    this.vendor = data.vendor;
    this.name = data.name;
    this.computeUtilization = data.computeUtilization;
    this.memoryUtilization = data.memoryUtilization;
    this.memoryUsedMegabytes = data.memoryUsedMegabytes;
    this.memoryTotalMegabytes = data.memoryTotalMegabytes;
  }
}

export class GpuUtilization {
  computeUtilization: number;
  memoryUtilization: number;
  memoryUsedMegabytes: number;
  memoryTotalMegabytes: number;

  constructor(data: Raw<GpuUtilization>) {
    this.computeUtilization = data.computeUtilization;
    this.memoryUtilization = data.memoryUtilization;
    this.memoryUsedMegabytes = data.memoryUsedMegabytes;
    this.memoryTotalMegabytes = data.memoryTotalMegabytes;
  }
}

export class GroupResourceLimitEntry {
  name: string;
  role: Role;
  resourceLimitation: ResourceLimitation;

  constructor(data: Raw<GroupResourceLimitEntry>) {
    this.name = data.name;
    this.role = data.role;
    this.resourceLimitation = data.resourceLimitation;
  }
}

export class MemInfo {
  memoryTotal: number;
  memoryFree: number;
  systemCache: number;
  swapTotal: number;
  swapFree: number;

  constructor(data: Raw<MemInfo>) {
    this.memoryTotal = data.memoryTotal;
    this.memoryFree = data.memoryFree;
    this.systemCache = data.systemCache;
    this.swapTotal = data.swapTotal;
    this.swapFree = data.swapFree;
  }
}

export class NetInfo {
  receiving: number;
  transmitting: number;

  constructor(data: Raw<NetInfo>) {
    this.receiving = data.receiving;
    this.transmitting = data.transmitting;
  }
}

export class NodeInfo {
  name: string;
  time: number;
  uptime: number;
  cpuInfo: CpuInfo;
  memInfo: MemInfo;
  netInfo: NetInfo;
  diskInfo: DiskInfo;
  gpuInfo: GpuInfo[];
  buildInfo: BuildInfo;
  allocInfo: AllocInfo[];

  constructor(data: Raw<NodeInfo>) {
    this.name = data.name;
    this.time = data.time;
    this.uptime = data.uptime;
    this.cpuInfo = data.cpuInfo;
    this.memInfo = data.memInfo;
    this.netInfo = data.netInfo;
    this.diskInfo = data.diskInfo;
    this.gpuInfo = data.gpuInfo;
    this.buildInfo = data.buildInfo;
    this.allocInfo = data.allocInfo;
  }
}

export class NodeResourceUsageConfiguration {
  freeForAll: boolean;
  globalLimit?: ResourceLimitation;
  groupLimits: GroupResourceLimitEntry[];

  constructor(data: Raw<NodeResourceUsageConfiguration>) {
    this.freeForAll = data.freeForAll;
    this.globalLimit = data.globalLimit;
    this.groupLimits = data.groupLimits;
  }
}

export class NodeUtilization {
  time: number;
  uptime: number;
  cpuUtilization: number[];
  memoryInfo: MemInfo;
  netInfo: NetInfo;
  diskInfo: DiskInfo;
  gpuUtilization: GpuUtilization[];

  constructor(data: Raw<NodeUtilization>) {
    this.time = data.time;
    this.uptime = data.uptime;
    this.cpuUtilization = data.cpuUtilization;
    this.memoryInfo = data.memoryInfo;
    this.netInfo = data.netInfo;
    this.diskInfo = data.diskInfo;
    this.gpuUtilization = data.gpuUtilization;
  }
}

export class DeletionPolicy {
  keepWorkspaceOfFailedStage: boolean;
  numberOfWorkspacesOfSucceededStagesToKeep?: number;
  alwaysKeepMostRecentWorkspace?: boolean;

  constructor(data: DeletionPolicy) {
    this.keepWorkspaceOfFailedStage = data.keepWorkspaceOfFailedStage;
    this.numberOfWorkspacesOfSucceededStagesToKeep = data.numberOfWorkspacesOfSucceededStagesToKeep;
    this.alwaysKeepMostRecentWorkspace = data.alwaysKeepMostRecentWorkspace;
  }
}

export class EnvVariable {
  key: string;
  value?: string;
  valueInherited?: string;

  constructor(data: Raw<EnvVariable>) {
    this.key = data.key;
    this.value = data.value;
    this.valueInherited = data.valueInherited;
  }
}

export class ExecutionGroupInfo {
  id: string;
  configureOnly: boolean;
  stageDefinition: StageDefinitionInfoUnion;
  rangedValues: Record<string, RangedValueUnion>;
  workspaceConfiguration: WorkspaceConfiguration;
  stages: StageInfo[];
  active: boolean;
  enqueued: boolean;
  comment?: string;
  enqueueIndex?: number;

  constructor(data: Raw<ExecutionGroupInfo>) {
    this.id = data.id;
    this.configureOnly = data.configureOnly;
    this.stageDefinition = data.stageDefinition;
    this.rangedValues = data.rangedValues;
    this.workspaceConfiguration = data.workspaceConfiguration;
    this.stages = data.stages;
    this.active = data.active;
    this.enqueued = data.enqueued;
    this.comment = data.comment;
    this.enqueueIndex = data.enqueueIndex;
  }

  rangedValuesKeys (): string[] {
    return Object.keys(this.rangedValues);
  };

  hasStagesState (state: State): boolean {
    for (const stage of this.stages) {
      if (stage.state === state) {
        return true;
      }
    }
    return false;
  };

  getMostRecentStage (): StageInfo | undefined {
    for (const stage of [...this.stages].reverse()) {
      if (stage.finishTime != null) {
        return stage;
      } else if (stage.startTime != null) {
        return stage;
      }
    }
    return undefined;
  };

  getMostRecentStartOrFinishTime (): number | undefined {
    const stage = this.getMostRecentStage();
    if (stage == undefined) {
      return undefined;
    } else if (stage.startTime != undefined) {
      return stage.startTime;
    } else if (stage?.finishTime != undefined) {
      return stage.finishTime;
    } else {
      return undefined;
    }
  };

  getMostRelevantState(projectState?: State): State {
    const states: Array<State> = ['RUNNING', 'PREPARING', 'FAILED'];
    for (const state of states) {
      if (this.hasStagesState(state)) {
        return state;
      }
    }
    if (this.enqueued) {
      return 'ENQUEUED';
    } else if (this.active) {
      const alternative = projectState === 'PAUSED' ? 'PAUSED' : 'PREPARING';
      return this.getMostRecentStage()?.state ?? alternative;
    } else {
      return this.getMostRecentStage()?.state ?? 'SKIPPED';
    }
  };

  isMostRecentStateRunning (): boolean {
    return this.getMostRelevantState() === 'RUNNING';
  };

  hasRunningStages (): boolean {
    for (const stage of this.stages) {
      if (stage.state === 'RUNNING') {
        return true;
      }
    }
    return false;
  };

  getGroupSize (): number {
    const rvKeys = Object.keys(this.rangedValues);

    if (rvKeys.length > 0) {
      let size = 0;
      for (const entry of Object.entries(this.rangedValues)) {
        size += (entry[1] as RangedValue).stepCount;
      }
      return size;
    } else {
      return 1;
    }
  };
}

export class GpuRequirementsInfo {
  count: number;
  vendor: string;
  support: string[];

  constructor(data: Raw<GpuRequirementsInfo>) {
    this.count = data.count;
    this.vendor = data.vendor;
    this.support = data.support;
  }
}

export class HighlightInfo {
  resources: string[];

  constructor(data: Raw<HighlightInfo>) {
    this.resources = data.resources;
  }
}

export class ImageInfo {
  name: string;
  args: string[];
  shmMegabytes: number;

  constructor(data: Raw<ImageInfo>) {
    this.name = data.name;
    this.args = data.args;
    this.shmMegabytes = data.shmMegabytes;
  }
}

export class LogEntryInfo {
  time: number;
  source: LogSource;
  error: boolean;
  message: string;
  line: number;
  stageId: string;

  constructor(data: Raw<LogEntryInfo>) {
    this.time = data.time;
    this.source = data.source;
    this.error = data.error;
    this.message = data.message;
    this.line = data.line;
    this.stageId = data.stageId;
  }
}

export class LogParserInfo {
  matcher: string;
  destination: string;
  formatter: string;
  type: string;

  constructor(data: Raw<LogParserInfo>) {
    this.matcher = data.matcher;
    this.destination = data.destination;
    this.formatter = data.formatter;
    this.type = data.type;
  }
}

export class ParseError {
  line: number;
  column: number;
  message: string;

  constructor(data: Raw<ParseError>) {
    this.line = data.line;
    this.column = data.column;
    this.message = data.message;
  }
}

export class PipelineDefinitionInfo {
  id: string;
  name: string;
  description: string;
  userInput: UserInputInfo;
  stages: StageDefinitionInfoUnion[];
  environment: Record<string, string>;
  deletionPolicy: DeletionPolicy;
  groups: Link[];
  belongsToProject: string | null;
  publicAccess: boolean;

  constructor(data: Raw<PipelineDefinitionInfo>) {
    this.id = data.id;
    this.name = data.name;
    this.description = data.description;
    this.userInput = data.userInput;
    this.stages = data.stages;
    this.environment = data.environment;
    this.deletionPolicy = data.deletionPolicy;
    this.groups = data.groups;
    this.belongsToProject = data.belongsToProject;
    this.publicAccess = data.publicAccess;
  }
}

export class RangeWithStepSize implements RangedValue {
  stepCount: number;
  '@type': 'DiscreteSteps';
  min: number;
  max: number;
  stepSize: number;

  constructor(data: Raw<RangeWithStepSize>) {
    this.stepCount = data.stepCount;
    this['@type'] = data['@type'];
    this.min = data.min;
    this.max = data.max;
    this.stepSize = data.stepSize;
  }
}

export class RangedList implements RangedValue {
  stepCount: number;
  '@type': 'List';
  values: string[];

  constructor(data: Raw<RangedList>) {
    this.stepCount = data.stepCount;
    this['@type'] = data['@type'];
    this.values = data.values;
  }
}

export interface RangedValue {
  '@type': 'DiscreteSteps' | 'List';
  stepCount: number;
}

export class RequirementsInfo {
  cpus: number;
  megabytesOfRam: number;
  gpu: GpuRequirementsInfo;
  tags: string[];

  constructor(data: Raw<RequirementsInfo>) {
    this.cpus = data.cpus;
    this.megabytesOfRam = data.megabytesOfRam;
    this.gpu = data.gpu;
    this.tags = data.tags;
  }
}

export class ResourceInfo {
  cpus: number;
  megabytesOfRam: number;
  gpus: number;

  constructor(data: Raw<ResourceInfo>) {
    this.cpus = data.cpus;
    this.megabytesOfRam = data.megabytesOfRam;
    this.gpus = data.gpus;
  }
}

export class StageAndGatewayDefinitionInfo implements StageGatewayDefinitionInfo {
  name: string;
  id: string;
  nextStages: string[];
  gatewaySubType: GatewaySubType;
  '@type': 'AndGateway';
  description: string;

  constructor(data: Raw<StageAndGatewayDefinitionInfo>) {
    this.name = data.name;
    this.id = data.id;
    this.nextStages = data.nextStages;
    this.gatewaySubType = data.gatewaySubType;
    this['@type'] = data['@type'];
    this.description = data.description;
  }
}

export interface StageDefinitionInfo {
  '@type': 'AndGateway' | 'XorGateway' | 'Worker';
  name: string;
  id: string;
  nextStages: string[];
}

export interface StageGatewayDefinitionInfo extends StageDefinitionInfo {
  '@type': 'AndGateway' | 'XorGateway';
  gatewaySubType: GatewaySubType;
}

export class StageInfo {
  id: string;
  startTime?: DateAsNumber;
  finishTime?: DateAsNumber;
  state?: State;
  workspace?: string;
  env: Record<string, string>;
  envPipeline: Record<string, string>;
  envSystem: Record<string, string>;
  envInternal: Record<string, string>;
  result: Record<string, string>;

  constructor(data: Raw<StageInfo>) {
    this.id = data.id;
    this.startTime = data.startTime;
    this.finishTime = data.finishTime;
    this.state = data.state;
    this.workspace = data.workspace;
    this.env = data.env;
    this.envPipeline = data.envPipeline;
    this.envSystem = data.envSystem;
    this.envInternal = data.envInternal;
    this.result = data.result;
  }
}

export class StageWorkerDefinitionInfo implements StageDefinitionInfo {
  name: string;
  id: string;
  nextStages: string[];
  '@type': 'Worker';
  description: string;
  image: ImageInfo;
  requiredResources: RequirementsInfo;
  userInput: UserInputInfo;
  environment: Record<string, string>;
  highlight: HighlightInfo;
  discardable: boolean;
  privileged: boolean;
  logParsers: LogParserInfo[];
  ignoreFailuresWithinExecutionGroup: boolean;

  constructor(data: Raw<StageWorkerDefinitionInfo>) {
    this.name = data.name;
    this.id = data.id;
    this.nextStages = data.nextStages;
    this['@type'] = data['@type'];
    this.description = data.description;
    this.image = data.image;
    this.requiredResources = data.requiredResources;
    this.userInput = data.userInput;
    this.environment = data.environment;
    this.highlight = data.highlight;
    this.discardable = data.discardable;
    this.privileged = data.privileged;
    this.logParsers = data.logParsers;
    this.ignoreFailuresWithinExecutionGroup = data.ignoreFailuresWithinExecutionGroup;
  }
}

export class StageXOrGatewayDefinitionInfo implements StageGatewayDefinitionInfo {
  name: string;
  id: string;
  nextStages: string[];
  gatewaySubType: GatewaySubType;
  '@type': 'XorGateway';
  description: string;
  conditions: string[];

  constructor(data: Raw<StageXOrGatewayDefinitionInfo>) {
    this.name = data.name;
    this.id = data.id;
    this.nextStages = data.nextStages;
    this.gatewaySubType = data.gatewaySubType;
    this['@type'] = data['@type'];
    this.description = data.description;
    this.conditions = data.conditions;
  }
}

export class StateInfo {
  state?: State;
  pauseReason?: string;
  description?: string;
  stageProgress?: number;
  hasEnqueuedStages: boolean;

  constructor(data: Raw<StateInfo>) {
    this.state = data.state;
    this.pauseReason = data.pauseReason;
    this.description = data.description;
    this.stageProgress = data.stageProgress;
    this.hasEnqueuedStages = data.hasEnqueuedStages;
  }
}

export class StatsInfo {
  stageId: string;
  nodeName: string;
  cpuUsed: number;
  cpuMaximum: number;
  memoryAllocated: number;
  memoryMaximum: number;

  constructor(data: Raw<StatsInfo>) {
    this.stageId = data.stageId;
    this.nodeName = data.nodeName;
    this.cpuUsed = data.cpuUsed;
    this.cpuMaximum = data.cpuMaximum;
    this.memoryAllocated = data.memoryAllocated;
    this.memoryMaximum = data.memoryMaximum;
  }
}

export class UserInputInfo {
  confirmation: Confirmation;
  requiredEnvVariables: string[];

  constructor(data: Raw<UserInputInfo>) {
    this.confirmation = data.confirmation;
    this.requiredEnvVariables = data.requiredEnvVariables;
  }
}

export class WorkspaceConfiguration {
  mode: WorkspaceMode;
  value?: string;
  sharedWithinGroup: boolean;
  nestedWithinGroup: boolean;

  constructor(data: Raw<WorkspaceConfiguration>) {
    this.mode = data.mode;
    this.value = data.value;
    this.sharedWithinGroup = data.sharedWithinGroup;
    this.nestedWithinGroup = data.nestedWithinGroup;
  }
}

export class AuthTokenInfo {
  id: string;
  secret?: string;
  name: string;
  capabilities: string[];

  constructor(data: Raw<AuthTokenInfo>) {
    this.id = data.id;
    this.secret = data.secret;
    this.name = data.name;
    this.capabilities = data.capabilities;
  }
}

export class EnqueueOnOtherRequest {
  id: string;
  env: Record<string, string>;
  rangedEnv?: Record<string, RangedValueUnion>;
  image?: ImageInfo;
  requiredResources?: ResourceInfo;
  workspaceConfiguration?: WorkspaceConfiguration;
  comment?: string;
  runSingle?: boolean;
  resume?: boolean;
  projectIds: string[];

  constructor(data: Raw<EnqueueOnOtherRequest>) {
    this.id = data.id;
    this.env = data.env;
    this.rangedEnv = data.rangedEnv;
    this.image = data.image;
    this.requiredResources = data.requiredResources;
    this.workspaceConfiguration = data.workspaceConfiguration;
    this.comment = data.comment;
    this.runSingle = data.runSingle;
    this.resume = data.resume;
    this.projectIds = data.projectIds;
  }
}

export class EnqueueRequest {
  id: string;
  env: Record<string, string>;
  rangedEnv?: Record<string, RangedValueUnion>;
  image?: ImageInfo;
  requiredResources?: ResourceInfo;
  workspaceConfiguration?: WorkspaceConfiguration;
  comment?: string;
  runSingle?: boolean;
  resume?: boolean;

  constructor(data: Raw<EnqueueRequest>) {
    this.id = data.id;
    this.env = data.env;
    this.rangedEnv = data.rangedEnv;
    this.image = data.image;
    this.requiredResources = data.requiredResources;
    this.workspaceConfiguration = data.workspaceConfiguration;
    this.comment = data.comment;
    this.runSingle = data.runSingle;
    this.resume = data.resume;
  }
}

export class LogLinesRequest {
  skipLines?: number;
  expectingStageId?: string;

  constructor(data: Raw<LogLinesRequest>) {
    this.skipLines = data.skipLines;
    this.expectingStageId = data.expectingStageId;
  }
}

export class ProjectCreateRequest {
  name: string;
  pipeline: string;
  tags?: string[];

  constructor(data: Raw<ProjectCreateRequest>) {
    this.name = data.name;
    this.pipeline = data.pipeline;
    this.tags = data.tags;
  }
}

export class ProjectInfo {
  id: string;
  accountingGroup: string;
  groups: Link[];
  tags: string[];
  name: string;
  publicAccess: boolean;
  pipelineDefinition: PipelineDefinitionInfo;

  constructor(data: Raw<ProjectInfo>) {
    this.id = data.id;
    this.accountingGroup = data.accountingGroup;
    this.groups = data.groups;
    this.tags = data.tags;
    this.name = data.name;
    this.publicAccess = data.publicAccess;
    this.pipelineDefinition = data.pipelineDefinition;
  }
}

export class UpdatePauseRequest {
  paused: boolean;
  strategy?: string;

  constructor(data: Raw<UpdatePauseRequest>) {
    this.paused = data.paused;
    this.strategy = data.strategy;
  }
}

export class ResourceLimitation {
  cpu?: number;
  mem?: number;
  gpu?: number;

  constructor(data: Raw<ResourceLimitation>) {
    this.cpu = data.cpu;
    this.mem = data.mem;
    this.gpu = data.gpu;
  }
}

export class StorageInfo {
  name: string;
  bytesUsed: number;
  bytesFree: number;

  constructor(data: Raw<StorageInfo>) {
    this.name = data.name;
    this.bytesUsed = data.bytesUsed;
    this.bytesFree = data.bytesFree;
  }
}

export type DateAsNumber = number;

export type Role = 'OWNER' | 'MAINTAINER' | 'MEMBER';

export type Action = 'EXECUTE' | 'CONFIGURE';

export type GatewaySubType = 'SPLITTER' | 'MERGER';

export type LogSource = 'STANDARD_IO' | 'MANAGEMENT_EVENT';

export type State = 'RUNNING' | 'PAUSED' | 'SUCCEEDED' | 'FAILED' | 'PREPARING' | 'ENQUEUED' | 'SKIPPED';

export type Confirmation = 'NEVER' | 'ONCE' | 'ALWAYS';

export type WorkspaceMode = 'STANDALONE' | 'INCREMENTAL' | 'CONTINUATION';

export type RangedValueUnion = RangeWithStepSize | RangedList;

export type StageDefinitionInfoUnion =
  StageWorkerDefinitionInfo
  | StageXOrGatewayDefinitionInfo
  | StageAndGatewayDefinitionInfo;

export function stageDefinitionIsWorker(def: StageDefinitionInfoUnion): def is StageWorkerDefinitionInfo {
  return (def as StageWorkerDefinitionInfo)["@type"] == "Worker"
}

export function stageDefinitionIsAndGateway(def: StageDefinitionInfoUnion): def is StageAndGatewayDefinitionInfo {
  return (def as StageAndGatewayDefinitionInfo)["@type"] == "AndGateway"
}

export function stageDefinitionIsXorGateway(def: StageDefinitionInfoUnion): def is StageXOrGatewayDefinitionInfo {
  return (def as StageXOrGatewayDefinitionInfo)["@type"] == "XorGateway"
}
