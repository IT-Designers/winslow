/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.0.1157 on 2022-11-28 17:08:16.

export class IBuild {

  constructor(data: IBuild) {
  }
}

export class IGroupInfo {
  name: string;
  members: ILink[];

  constructor(data: IGroupInfo) {
    this.name = data.name;
    this.members = data.members;
  }
}

export class ILink {
  name: string;
  role: IRole;

  constructor(data: ILink) {
    this.name = data.name;
    this.role = data.role;
  }
}

export class IUserInfo {
  name: string;

  constructor(data: IUserInfo) {
    this.name = data.name;
  }
}

export class IFileInfo {
  name: string;
  directory: boolean;
  path: string;
  fileSize?: number;
  attributes: { [index: string]: any };

  constructor(data: IFileInfo) {
    this.name = data.name;
    this.directory = data.directory;
    this.path = data.path;
    this.fileSize = data.fileSize;
    this.attributes = data.attributes;
  }
}

export class IAllocInfo {
  title: string;
  cpu: number;
  memory: number;
  gpu: number;

  constructor(data: IAllocInfo) {
    this.title = data.title;
    this.cpu = data.cpu;
    this.memory = data.memory;
    this.gpu = data.gpu;
  }
}

export class IBuildInfo {
  date: string;
  commitHashShort: string;
  commitHashLong: string;

  constructor(data: IBuildInfo) {
    this.date = data.date;
    this.commitHashShort = data.commitHashShort;
    this.commitHashLong = data.commitHashLong;
  }
}

export class ICpuInfo {
  modelName: string;
  utilization: number[];

  constructor(data: ICpuInfo) {
    this.modelName = data.modelName;
    this.utilization = data.utilization;
  }
}

export class ICpuUtilization {
  cpus: IIterable<number>;

  constructor(data: ICpuUtilization) {
    this.cpus = data.cpus;
  }
}

export class IDiskInfo {
  reading: number;
  writing: number;
  free: number;
  used: number;

  constructor(data: IDiskInfo) {
    this.reading = data.reading;
    this.writing = data.writing;
    this.free = data.free;
    this.used = data.used;
  }
}

export class IGpuInfo {
  id: string;
  vendor: string;
  name: string;
  computeUtilization: number;
  memoryUtilization: number;
  memoryUsedMegabytes: number;
  memoryTotalMegabytes: number;

  constructor(data: IGpuInfo) {
    this.id = data.id;
    this.vendor = data.vendor;
    this.name = data.name;
    this.computeUtilization = data.computeUtilization;
    this.memoryUtilization = data.memoryUtilization;
    this.memoryUsedMegabytes = data.memoryUsedMegabytes;
    this.memoryTotalMegabytes = data.memoryTotalMegabytes;
  }
}

export class IGpuUtilization {
  computeUtilization: number;
  memoryUtilization: number;
  memoryUsedMegabytes: number;
  memoryTotalMegabytes: number;

  constructor(data: IGpuUtilization) {
    this.computeUtilization = data.computeUtilization;
    this.memoryUtilization = data.memoryUtilization;
    this.memoryUsedMegabytes = data.memoryUsedMegabytes;
    this.memoryTotalMegabytes = data.memoryTotalMegabytes;
  }
}

export class IMemInfo {
  memoryTotal: number;
  memoryFree: number;
  systemCache: number;
  swapTotal: number;
  swapFree: number;

  constructor(data: IMemInfo) {
    this.memoryTotal = data.memoryTotal;
    this.memoryFree = data.memoryFree;
    this.systemCache = data.systemCache;
    this.swapTotal = data.swapTotal;
    this.swapFree = data.swapFree;
  }
}

export class INetInfo {
  transmitting: number;
  receiving: number;

  constructor(data: INetInfo) {
    this.transmitting = data.transmitting;
    this.receiving = data.receiving;
  }
}

export class INodeInfo {
  name: string;
  time?: number;
  uptime?: number;
  cpuInfo: ICpuInfo;
  memInfo: IMemInfo;
  netInfo: INetInfo;
  diskInfo: IDiskInfo;
  gpuInfo: IGpuInfo[];
  allocInfo?: IAllocInfo[];
  buildInfo: IBuildInfo;

  constructor(data: INodeInfo) {
    this.name = data.name;
    this.time = data.time;
    this.uptime = data.uptime;
    this.cpuInfo = data.cpuInfo;
    this.memInfo = data.memInfo;
    this.netInfo = data.netInfo;
    this.diskInfo = data.diskInfo;
    this.gpuInfo = data.gpuInfo;
    this.allocInfo = data.allocInfo;
    this.buildInfo = data.buildInfo;
  }
}

export class INodeUtilization {
  time: number;
  uptime: number;
  cpuUtilization: number[];
  memoryInfo: IMemInfo;
  netInfo: INetInfo;
  diskInfo: IDiskInfo;
  gpuUtilization: IGpuUtilization[];

  constructor(data: INodeUtilization) {
    this.time = data.time;
    this.uptime = data.uptime;
    this.cpuUtilization = data.cpuUtilization;
    this.memoryInfo = data.memoryInfo;
    this.netInfo = data.netInfo;
    this.diskInfo = data.diskInfo;
    this.gpuUtilization = data.gpuUtilization;
  }
}

export class IDeletionPolicy {
  keepWorkspaceOfFailedStage: boolean;
  numberOfWorkspacesOfSucceededStagesToKeep?: number;
  alwaysKeepMostRecentWorkspace?: boolean;

  constructor(data: IDeletionPolicy) {
    this.keepWorkspaceOfFailedStage = data.keepWorkspaceOfFailedStage;
    this.numberOfWorkspacesOfSucceededStagesToKeep = data.numberOfWorkspacesOfSucceededStagesToKeep;
    this.alwaysKeepMostRecentWorkspace = data.alwaysKeepMostRecentWorkspace;
  }
}

export class IEnvVariable {
  key: string;
  value?: string;
  valueInherited?: string;

  constructor(data: IEnvVariable) {
    this.key = data.key;
    this.value = data.value;
    this.valueInherited = data.valueInherited;
  }
}

export class IExecutionGroupInfo {
  id: string;
  configureOnly: boolean;
  stageDefinition: IStageDefinitionInfo;
  rangedValues: { [index: string]: any };
  workspaceConfiguration: IWorkspaceConfiguration;
  stages: IStageInfo[];
  active: boolean;
  enqueued: boolean;
  comment?: string;

  constructor(data: IExecutionGroupInfo) {
    this.id = data.id;
    this.configureOnly = data.configureOnly;
    this.stageDefinition = data.stageDefinition;
    this.rangedValues = data.rangedValues;
    this.workspaceConfiguration = data.workspaceConfiguration;
    this.stages = data.stages;
    this.active = data.active;
    this.enqueued = data.enqueued;
    this.comment = data.comment;
  }
}

export class IImageInfo {
  name?: string;
  args?: string[];
  shmMegabytes?: number;

  constructor(data: IImageInfo) {
    this.name = data.name;
    this.args = data.args;
    this.shmMegabytes = data.shmMegabytes;
  }
}

export class ILogEntry {
  time: number;
  source: ISource;
  error: boolean;
  message: string;

  constructor(data: ILogEntry) {
    this.time = data.time;
    this.source = data.source;
    this.error = data.error;
    this.message = data.message;
  }
}

export class ILogEntryInfo extends ILogEntry {
  line: number;
  stageId?: string;

  constructor(data: ILogEntryInfo) {
    super(data);
    this.line = data.line;
    this.stageId = data.stageId;
  }
}

export class IParseError {
  line: number;
  column: number;
  message: string;

  constructor(data: IParseError) {
    this.line = data.line;
    this.column = data.column;
    this.message = data.message;
  }
}

export class IPipelineInfo {
  id: string;
  name: string;
  desc?: string;
  requiredEnvVariables: string[];
  stages: IStageDefinitionInfo[];
  markers: string[];

  constructor(data: IPipelineInfo) {
    this.id = data.id;
    this.name = data.name;
    this.desc = data.desc;
    this.requiredEnvVariables = data.requiredEnvVariables;
    this.stages = data.stages;
    this.markers = data.markers;
  }
}

export class IResourceInfo {
  cpus: number;
  megabytesOfRam: number;
  gpus?: number;

  constructor(data: IResourceInfo) {
    this.cpus = data.cpus;
    this.megabytesOfRam = data.megabytesOfRam;
    this.gpus = data.gpus;
  }
}

export class IStageDefinitionInfo {
  name: string;
  image?: IImageInfo;
  requiredEnvVariables: string[];
  requiredResources?: IResourceInfo;
  env?: { [index: string]: string };

  constructor(data: IStageDefinitionInfo) {
    this.name = data.name;
    this.image = data.image;
    this.requiredEnvVariables = data.requiredEnvVariables;
    this.requiredResources = data.requiredResources;
    this.env = data.env;
  }
}

export class IStageInfo {
  id: string;
  startTime?: DateAsNumber;
  finishTime?: DateAsNumber;
  state?: IState;
  workspace?: string;
  env: { [index: string]: string };
  envPipeline: { [index: string]: string };
  envSystem: { [index: string]: string };
  envInternal: { [index: string]: string };

  constructor(data: IStageInfo) {
    this.id = data.id;
    this.startTime = data.startTime;
    this.finishTime = data.finishTime;
    this.state = data.state;
    this.workspace = data.workspace;
    this.env = data.env;
    this.envPipeline = data.envPipeline;
    this.envSystem = data.envSystem;
    this.envInternal = data.envInternal;
  }
}

export class IStateInfo {
  state?: IState;
  pauseReason?: string;
  description?: string;
  stageProgress?: number;
  hasEnqueuedStages: boolean;

  constructor(data: IStateInfo) {
    this.state = data.state;
    this.pauseReason = data.pauseReason;
    this.description = data.description;
    this.stageProgress = data.stageProgress;
    this.hasEnqueuedStages = data.hasEnqueuedStages;
  }
}

export class IStats {
  stageId?: string;
  runningOnNode?: string;
  cpuUsed: number;
  cpuMaximum: number;
  memoryAllocated: number;
  memoryMaximum: number;

  constructor(data: IStats) {
    this.stageId = data.stageId;
    this.runningOnNode = data.runningOnNode;
    this.cpuUsed = data.cpuUsed;
    this.cpuMaximum = data.cpuMaximum;
    this.memoryAllocated = data.memoryAllocated;
    this.memoryMaximum = data.memoryMaximum;
  }
}

export class IWorkspaceConfiguration {
  mode: IWorkspaceMode;
  value?: string;
  sharedWithinGroup?: boolean;
  nestedWithinGroup?: boolean;

  constructor(data: IWorkspaceConfiguration) {
    this.mode = data.mode;
    this.value = data.value;
    this.sharedWithinGroup = data.sharedWithinGroup;
    this.nestedWithinGroup = data.nestedWithinGroup;
  }
}

export class IAuthTokenInfo {
  id: string;
  secret?: string;
  name: string;
  capabilities: string[];

  constructor(data: IAuthTokenInfo) {
    this.id = data.id;
    this.secret = data.secret;
    this.name = data.name;
    this.capabilities = data.capabilities;
  }
}

export class IEnqueueRequest {
  env: { [index: string]: string };
  rangedEnv?: { [index: string]: any };
  stageIndex: number;
  image?: IImageInfo;
  requiredResources?: IResourceInfo;
  workspaceConfiguration?: IWorkspaceConfiguration;
  comment?: string;
  runSingle?: boolean;
  resume?: boolean;

  constructor(data: IEnqueueRequest) {
    this.env = data.env;
    this.rangedEnv = data.rangedEnv;
    this.stageIndex = data.stageIndex;
    this.image = data.image;
    this.requiredResources = data.requiredResources;
    this.workspaceConfiguration = data.workspaceConfiguration;
    this.comment = data.comment;
    this.runSingle = data.runSingle;
    this.resume = data.resume;
  }
}

export class IEnqueueOnOtherRequest extends IEnqueueRequest {
  projectIds: string[];

  constructor(data: IEnqueueOnOtherRequest) {
    super(data);
    this.projectIds = data.projectIds;
  }
}

export class ILogLinesRequest {
  skipLines?: number;
  expectingStageId?: string;

  constructor(data: ILogLinesRequest) {
    this.skipLines = data.skipLines;
    this.expectingStageId = data.expectingStageId;
  }
}

export class IProjectCreateRequest {
  name: string;
  pipeline: string;
  tags?: string[];

  constructor(data: IProjectCreateRequest) {
    this.name = data.name;
    this.pipeline = data.pipeline;
    this.tags = data.tags;
  }
}

export class IProjectInfo {
  id: string;
  owner: string;
  groups: string[];
  tags: string[];
  name: string;
  publicAccess: boolean;
  pipelineDefinition: IPipelineInfo;

  constructor(data: IProjectInfo) {
    this.id = data.id;
    this.owner = data.owner;
    this.groups = data.groups;
    this.tags = data.tags;
    this.name = data.name;
    this.publicAccess = data.publicAccess;
    this.pipelineDefinition = data.pipelineDefinition;
  }
}

export class IUpdatePauseRequest {
  paused: boolean;
  strategy?: string;

  constructor(data: IUpdatePauseRequest) {
    this.paused = data.paused;
    this.strategy = data.strategy;
  }
}

export class IResourceLimitation {
  cpu?: number;
  mem?: number;
  gpu?: number;

  constructor(data: IResourceLimitation) {
    this.cpu = data.cpu;
    this.mem = data.mem;
    this.gpu = data.gpu;
  }
}

export class IStorageInfo {
  name?: string;
  bytesUsed: number;
  bytesFree: number;

  constructor(data: IStorageInfo) {
    this.name = data.name;
    this.bytesUsed = data.bytesUsed;
    this.bytesFree = data.bytesFree;
  }
}

export interface IIterable<T> {
}

export type DateAsNumber = number;

export type IRole = "OWNER" | "MEMBER";

export type IAction = "Execute" | "Configure";

export type ISource = "STANDARD_IO" | "MANAGEMENT_EVENT";

export type IState = "Running" | "Paused" | "Succeeded" | "Failed" | "Preparing" | "Enqueued" | "Skipped";

export type IWorkspaceMode = "STANDALONE" | "INCREMENTAL" | "CONTINUATION";
