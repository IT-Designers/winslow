/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.0.1157 on 2022-11-22 14:43:02.

export interface IBuild {
}

export class IFileInfo {
    name: string;
    directory: boolean;
    path: string;
    fileSize: number;
    attributes: { [index: string]: any };
}

export class IAllocInfo {
    title: string;
    cpu: number;
    memory: number;
    gpu: number;
}

export class IBuildInfo {
    date: string;
    commitHashShort: string;
    commitHashLong: string;
}

export class ICpuInfo {
    modelName: string;
    utilization: number[];
}

export class ICpuUtilization {
    cpus: IIterable<number>;
}

export class IDiskInfo {
    reading: number;
    writing: number;
    free: number;
    used: number;
}

export class IGpuInfo {
    id: string;
    vendor: string;
    name: string;
    computeUtilization: number;
    memoryUtilization: number;
    memoryUsedMegabytes: number;
    memoryTotalMegabytes: number;
}

export class IGpuUtilization {
    computeUtilization: number;
    memoryUtilization: number;
    memoryUsedMegabytes: number;
    memoryTotalMegabytes: number;
}

export class IMemInfo {
    memoryTotal: number;
    memoryFree: number;
    systemCache: number;
    swapTotal: number;
    swapFree: number;
}

export class INetInfo {
    transmitting: number;
    receiving: number;
}

export class INodeInfo {
    name: string;
    time: number;
    uptime: number;
    cpuInfo: ICpuInfo;
    memInfo: IMemInfo;
    netInfo: INetInfo;
    diskInfo: IDiskInfo;
    gpuInfo: IGpuInfo[];
    allocInfo: IAllocInfo[];
    buildInfo: IBuildInfo;
}

export class INodeUtilization {
    time: number;
    uptime: number;
    cpuUtilization: number[];
    memoryInfo: IMemInfo;
    netInfo: INetInfo;
    diskInfo: IDiskInfo;
    gpuUtilization: IGpuUtilization[];
}

export interface IDeletionPolicy {
    keepWorkspaceOfFailedStage: boolean;
    numberOfWorkspacesOfSucceededStagesToKeep?: number;
    alwaysKeepMostRecentWorkspace: boolean;
}

export interface IEnvVariable {
    key: string;
    value: string;
    valueInherited: string;
}

export interface IExecutionGroupInfo {
    id: string;
    configureOnly: boolean;
    stageDefinition: IStageDefinitionInfo;
    rangedValues: { [index: string]: IRangedValue };
    workspaceConfiguration: IWorkspaceConfiguration;
    stages: IStageInfo[];
    active: boolean;
    enqueued: boolean;
    comment: string;
}

export interface IImageInfo {
    name: string;
    args: string[];
    shmMegabytes: number;
}

export interface ILogEntry {
    time: number;
    source: ISource;
    error: boolean;
    message: string;
}

export interface ILogEntryInfo extends ILogEntry {
    line: number;
    stageId: string;
}

export interface IParseError {
    line: number;
    column: number;
    message: string;
}

export interface IPipelineInfo {
    id: string;
    name: string;
    desc: string;
    requiredEnvVariables: string[];
    stages: IStageDefinitionInfo[];
    markers: string[];
}

export interface IRangeWithStepSize extends IRangedValue {
    min: number;
    max: number;
    stepSize: number;
}

export interface IRangedList extends IRangedValue {
    values: string[];
}

export interface IRangedValue {
    stepCount: number;
}

export interface IResourceInfo {
    cpus: number;
    megabytesOfRam: number;
    gpus: number;
}

export class IStageDefinitionInfo {
    name: string;
    image: IImageInfo;
    requiredEnvVariables: string[];
    requiredResources: IResourceInfo;
    env: { [index: string]: string };
}

export interface IStageInfo {
    id: string;
    startTime: Date;
    finishTime: Date;
    state: IState;
    workspace: string;
    env: { [index: string]: string };
    envPipeline: { [index: string]: string };
    envSystem: { [index: string]: string };
    envInternal: { [index: string]: string };
}

export interface IStateInfo {
    state: IState;
    pauseReason: string;
    description: string;
    stageProgress: number;
    hasEnqueuedStages: boolean;
}

export interface IStats {
    stageId: string;
    runningOnNode: string;
    cpuUsed: number;
    cpuMaximum: number;
    memoryAllocated: number;
    memoryMaximum: number;
}

export interface IWorkspaceConfiguration {
    mode: IWorkspaceMode;
    value?: string;
    sharedWithinGroup: boolean;
    nestedWithinGroup: boolean;
}

export interface IAuthTokenInfo {
    id: string;
    secret: string;
    name: string;
    capabilities: string[];
}

export interface IEnqueueOnOtherRequest extends IEnqueueRequest {
    projectIds: string[];
}

export interface IEnqueueRequest {
    env: { [index: string]: string };
    rangedEnv: { [index: string]: IRangedValue };
    stageIndex: number;
    image: IImageInfo;
    requiredResources: IResourceInfo;
    workspaceConfiguration: IWorkspaceConfiguration;
    comment: string;
    runSingle: boolean;
    resume: boolean;
}

export interface ILogLinesRequest {
    skipLines: number;
    expectingStageId: string;
}

export interface IProjectCreateRequest {
    name: string;
    pipeline: string;
    tags: string[];
}

export interface IProjectInfo {
    id: string;
    owner: string;
    groups: string[];
    tags: string[];
    name: string;
    publicAccess: boolean;
    pipelineDefinition: IPipelineInfo;
}

export interface IUpdatePauseRequest {
    paused: boolean;
    strategy: string;
}

export interface IResourceLimitation {
    cpu: number;
    mem: number;
    gpu: number;
}

export interface IStorageInfo {
    name: string;
    bytesUsed: number;
    bytesFree: number;
}

export interface IIterable<T> {
}

export type IAction = "Execute" | "Configure";

export type ISource = "STANDARD_IO" | "MANAGEMENT_EVENT";

export type IState = "Running" | "Paused" | "Succeeded" | "Failed" | "Preparing";

export type IWorkspaceMode = "STANDALONE" | "INCREMENTAL" | "CONTINUATION";
