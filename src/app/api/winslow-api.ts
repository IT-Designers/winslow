/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.0.1157 on 2022-12-06 15:06:38.

export class Build {

  constructor(data: Build) {
  }
}

export class GroupInfo {
  name: string;
  members: Link[];

  constructor(data: GroupInfo) {
    this.name = data.name;
    this.members = data.members;
  }
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

  constructor(data: UserInfo) {
    this.name = data.name;
  }
}

export class FileInfo {
  name: string;
  directory: boolean;
  path: string;
  fileSize?: number;
  attributes: { [index: string]: any };

  constructor(data: FileInfo) {
    this.name = data.name;
    this.directory = data.directory;
    this.path = data.path;
    this.fileSize = data.fileSize;
    this.attributes = data.attributes;
  }
}

export class AllocInfo {
  title: string;
  cpu: number;
  memory: number;
  gpu: number;

  constructor(data: AllocInfo) {
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

  constructor(data: BuildInfo) {
    this.date = data.date;
    this.commitHashShort = data.commitHashShort;
    this.commitHashLong = data.commitHashLong;
  }
}

export class CpuInfo {
  modelName: string;
  utilization: number[];

  constructor(data: CpuInfo) {
    this.modelName = data.modelName;
    this.utilization = data.utilization;
  }
}

export class CpuUtilization {
  cpus: Iterable<number>;

  constructor(data: CpuUtilization) {
    this.cpus = data.cpus;
  }
}

export class DiskInfo {
  reading: number;
  writing: number;
  free: number;
  used: number;

  constructor(data: DiskInfo) {
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

  constructor(data: GpuInfo) {
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

  constructor(data: GpuUtilization) {
    this.computeUtilization = data.computeUtilization;
    this.memoryUtilization = data.memoryUtilization;
    this.memoryUsedMegabytes = data.memoryUsedMegabytes;
    this.memoryTotalMegabytes = data.memoryTotalMegabytes;
  }
}

export class MemInfo {
  memoryTotal: number;
  memoryFree: number;
  systemCache: number;
  swapTotal: number;
  swapFree: number;

  constructor(data: MemInfo) {
    this.memoryTotal = data.memoryTotal;
    this.memoryFree = data.memoryFree;
    this.systemCache = data.systemCache;
    this.swapTotal = data.swapTotal;
    this.swapFree = data.swapFree;
  }
}

export class NetInfo {
  transmitting: number;
  receiving: number;

  constructor(data: NetInfo) {
    this.transmitting = data.transmitting;
    this.receiving = data.receiving;
  }
}

export class NodeInfo {
  name: string;
  time?: number;
  uptime?: number;
  cpuInfo: CpuInfo;
  memInfo: MemInfo;
  netInfo: NetInfo;
  diskInfo: DiskInfo;
  gpuInfo: GpuInfo[];
  allocInfo?: AllocInfo[];
  buildInfo: BuildInfo;

  constructor(data: NodeInfo) {
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

export class NodeUtilization {
  time: number;
  uptime: number;
  cpuUtilization: number[];
  memoryInfo: MemInfo;
  netInfo: NetInfo;
  diskInfo: DiskInfo;
  gpuUtilization: GpuUtilization[];

  constructor(data: NodeUtilization) {
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

  constructor(data: EnvVariable) {
    this.key = data.key;
    this.value = data.value;
    this.valueInherited = data.valueInherited;
  }
}

export class ExecutionGroupInfo {
  id: string;
  configureOnly: boolean;
  stageDefinition: StageDefinitionInfo;
  rangedValues: { [index: string]: any };
  workspaceConfiguration: WorkspaceConfiguration;
  stages: StageInfo[];
  active: boolean;
  enqueued: boolean;
  comment?: string;

  constructor(data: ExecutionGroupInfo) {
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

export class HighlightInfo {
  resources: string[];

  constructor(data: HighlightInfo) {
    this.resources = data.resources;
  }
}

export class ImageInfo {
  name?: string;
  args?: string[];
  shmMegabytes?: number;

  constructor(data: ImageInfo) {
    this.name = data.name;
    this.args = data.args;
    this.shmMegabytes = data.shmMegabytes;
  }
}

export class LogEntry {
  time: number;
  source: Source;
  error: boolean;
  message: string;

  constructor(data: LogEntry) {
    this.time = data.time;
    this.source = data.source;
    this.error = data.error;
    this.message = data.message;
  }
}

export class LogEntryInfo extends LogEntry {
  line: number;
  stageId?: string;

  constructor(data: LogEntryInfo) {
    super(data);
    this.line = data.line;
    this.stageId = data.stageId;
  }
}

export class LogParserInfo {
  matcher: string;
  destination: string;
  formatter: string;
  type: string;

  constructor(data: LogParserInfo) {
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

  constructor(data: ParseError) {
    this.line = data.line;
    this.column = data.column;
    this.message = data.message;
  }
}


export class PipelineDefinitionInfo {
  id: string;
  name: string;
  description?: string;
  userInput: UserInputInfo;
  stages: StageDefinitionInfo[];
  deletionPolicy: DeletionPolicy;
  markers: string[];

  constructor(data: PipelineDefinitionInfo) {
    this.id = data.id;
    this.name = data.name;
    this.description = data.description;
    this.userInput = data.userInput;
    this.stages = data.stages;
    this.deletionPolicy = data.deletionPolicy;
    this.markers = data.markers;
  }
}


export class RequirementsInfo {
  cpus: number;
  megabytesOfRam: number;
  gpu: GPUInfo;
  tags: string[];

  constructor(data: RequirementsInfo) {
    this.cpus = data.cpus;
    this.megabytesOfRam = data.megabytesOfRam;
    this.gpu = data.gpu;
    this.tags = data.tags;
  }
}

export class GPUInfo {
  count: number;
  vendor: string;
  support: string[];

  constructor(data: GPUInfo) {
    this.count = data.count;
    this.vendor = data.vendor;
    this.support = data.support;
  }
}

export class ResourceInfo {
  cpus: number;
  megabytesOfRam: number;
  gpus?: number;

  constructor(data: ResourceInfo) {
    this.cpus = data.cpus;
    this.megabytesOfRam = data.megabytesOfRam;
    this.gpus = data.gpus;
  }
}

export class StageAndGatewayDefinitionInfo implements StageDefinitionInfo {
  id: string;
  name: string;
  description: string;
  nextStages: string[];

  constructor(data: StageAndGatewayDefinitionInfo) {
    this.id = data.id;
    this.name = data.name;
    this.description = data.description;
    this.nextStages = data.nextStages;
  }
}

export interface StageDefinitionInfo {
  id: string;
  name: string;

  nextStages: string[];
}

export class StageInfo {
  id: string;
  startTime?: DateAsNumber;
  finishTime?: DateAsNumber;
  state?: State;
  workspace?: string;
  env: { [index: string]: string };
  envPipeline: { [index: string]: string };
  envSystem: { [index: string]: string };
  envInternal: { [index: string]: string };
  result: { [index: string]: string };

  constructor(data: StageInfo) {
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
  id: string;
  name: string;
  description?: string;
  image?: ImageInfo;
  requiredResources?: RequirementsInfo;
  userInput?: UserInputInfo;
  environment?: { [index: string]: string };
  highlight?: HighlightInfo;
  discardable?: boolean;
  privileged?: boolean;
  logParsers?: LogParserInfo[];
  ignoreFailuresWithinExecutionGroup: boolean;
  nextStages: string[];

  constructor(data: StageWorkerDefinitionInfo) {
    this.id = data.id;
    this.name = data.name;
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
    this.nextStages = data.nextStages;
  }
}

export class StageXOrGatewayDefintionInfo implements StageDefinitionInfo {
  id: string;
  name: string;
  description: string;
  conditions: string[];
  nextStages: string[];

  constructor(data: StageXOrGatewayDefintionInfo) {
    this.id = data.id;
    this.name = data.name;
    this.description = data.description;
    this.conditions = data.conditions;
    this.nextStages = data.nextStages;
  }
}

export class StateInfo {
  state?: State;
  pauseReason?: string;
  description?: string;
  stageProgress?: number;
  hasEnqueuedStages: boolean;

  constructor(data: StateInfo) {
    this.state = data.state;
    this.pauseReason = data.pauseReason;
    this.description = data.description;
    this.stageProgress = data.stageProgress;
    this.hasEnqueuedStages = data.hasEnqueuedStages;
  }
}

export class Stats {
  stageId?: string;
  runningOnNode?: string;
  cpuUsed: number;
  cpuMaximum: number;
  memoryAllocated: number;
  memoryMaximum: number;

  constructor(data: Stats) {
    this.stageId = data.stageId;
    this.runningOnNode = data.runningOnNode;
    this.cpuUsed = data.cpuUsed;
    this.cpuMaximum = data.cpuMaximum;
    this.memoryAllocated = data.memoryAllocated;
    this.memoryMaximum = data.memoryMaximum;
  }
}

export class UserInputInfo {
  confirmation: Confirmation;
  environment: string[];

  constructor(data: UserInputInfo) {
    this.confirmation = data.confirmation;
    this.environment = data.environment;
  }
}

export class WorkspaceConfiguration {
  mode: WorkspaceMode;
  value?: string;
  sharedWithinGroup?: boolean;
  nestedWithinGroup?: boolean;

  constructor(data: WorkspaceConfiguration) {
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

  constructor(data: AuthTokenInfo) {
    this.id = data.id;
    this.secret = data.secret;
    this.name = data.name;
    this.capabilities = data.capabilities;
  }
}

export class EnqueueRequest {
  env: { [index: string]: string };
  rangedEnv?: { [index: string]: any };
  id: string;
  image?: ImageInfo;
  requiredResources?: ResourceInfo;
  workspaceConfiguration?: WorkspaceConfiguration;
  comment?: string;
  runSingle?: boolean;
  resume?: boolean;

  constructor(data: EnqueueRequest) {
    this.env = data.env;
    this.rangedEnv = data.rangedEnv;
    this.id = data.id;
    this.image = data.image;
    this.requiredResources = data.requiredResources;
    this.workspaceConfiguration = data.workspaceConfiguration;
    this.comment = data.comment;
    this.runSingle = data.runSingle;
    this.resume = data.resume;
  }
}

export class EnqueueOnOtherRequest extends EnqueueRequest {
  projectIds: string[];

  constructor(data: EnqueueOnOtherRequest) {
    super(data);
    this.projectIds = data.projectIds;
  }
}

export class LogLinesRequest {
  skipLines?: number;
  expectingStageId?: string;

  constructor(data: LogLinesRequest) {
    this.skipLines = data.skipLines;
    this.expectingStageId = data.expectingStageId;
  }
}

export class ProjectCreateRequest {
  name: string;
  pipeline: string;
  tags?: string[];

  constructor(data: ProjectCreateRequest) {
    this.name = data.name;
    this.pipeline = data.pipeline;
    this.tags = data.tags;
  }
}

export class ProjectInfo {
  id: string;
  owner: string;
  groups: string[];
  tags: string[];
  name: string;
  publicAccess: boolean;
  pipelineDefinition: PipelineDefinitionInfo;

  constructor(data: ProjectInfo) {
    this.id = data.id;
    this.owner = data.owner;
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

  constructor(data: UpdatePauseRequest) {
    this.paused = data.paused;
    this.strategy = data.strategy;
  }
}

export class ResourceLimitation {
  cpu?: number;
  mem?: number;
  gpu?: number;

  constructor(data: ResourceLimitation) {
    this.cpu = data.cpu;
    this.mem = data.mem;
    this.gpu = data.gpu;
  }
}

export class StorageInfo {
  name?: string;
  bytesUsed: number;
  bytesFree: number;

  constructor(data: StorageInfo) {
    this.name = data.name;
    this.bytesUsed = data.bytesUsed;
    this.bytesFree = data.bytesFree;
  }
}

export interface Iterable<T> {
}

export type DateAsNumber = number;

export type Role = "OWNER" | "MEMBER";

export type Action = "Execute" | "Configure";

export type Source = "STANDARD_IO" | "MANAGEMENT_EVENT";

export type State = "Running" | "Paused" | "Succeeded" | "Failed" | "Preparing" | "Enqueued" | "Skipped";

export type Confirmation = "Never" | "Once" | "Always";

export type WorkspaceMode = "STANDALONE" | "INCREMENTAL" | "CONTINUATION";
