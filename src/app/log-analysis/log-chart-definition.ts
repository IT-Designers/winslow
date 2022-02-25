import {StageInfo} from "../api/project-api.service";
import {BehaviorSubject, combineLatest, Observable} from "rxjs";
import {FilesApiService} from "../api/files-api.service";
import {map, switchMap} from "rxjs/operators";

export interface StageCsvInfo {
  id: string;
  stage: StageInfo;
  csvFile$s: BehaviorSubject<CsvFileInfo>[];
}

export enum CsvFileStatus {
  LOADING,
  OK,
  FILE_IS_EMPTY_OR_MISSING,
  FAILED,
}

export interface CsvFileInfo {

  filename: string;
  directory: string;
  status: CsvFileStatus;
  content: CsvFileContent;
}

export type CsvFileContent = string[][]

export class CsvFileController {
  static readonly PATH_TO_WORKSPACES = '/workspaces';

  private readonly filesApi: FilesApiService;

  stages$: BehaviorSubject<StageCsvInfo[]>;

  constructor(api) {
    this.filesApi = api;
    this.stages$ = new BehaviorSubject<StageCsvInfo[]>(null);
  }

  getFileSubject(stageId: string, filename: string) {

    return this.stages$.pipe(
      map(stages => stages.find(stage => stage.id == stageId)),
      switchMap(stage => {
        let file$ = stage.csvFile$s.find(file => file.getValue().filename == filename)
        if (file$ == null) {
          file$ = this.createFileSubject(stage, filename);
          stage.csvFile$s.push(file$);
        }
        return file$;
      })
    );

  }

  private createFileSubject(stageCsvInfo: StageCsvInfo, filename: string) {
    const directory = `${CsvFileController.PATH_TO_WORKSPACES}/${stageCsvInfo.stage.workspace}`;
    const filepath = `${directory}/${filename}`;

    const csvFile: CsvFileInfo = {
      content: [],
      status: CsvFileStatus.LOADING,
      directory: directory,
      filename: filename,
    }

    let file$ = new BehaviorSubject(csvFile);

    console.log(`Loading file ${filename} from ${directory}`);

    this.filesApi.getFile(filepath).toPromise()
      .then(text => {
        csvFile.content = this.parseCsv(text);
        csvFile.status = CsvFileStatus.OK;
        console.log(`Finished loading file ${filename} from ${directory}`)

        if (text.trim().length == 0) {
          csvFile.status = CsvFileStatus.FILE_IS_EMPTY_OR_MISSING;
          console.warn(`File ${filename} from ${directory} is empty or might be missing.`);
        }

        file$.next(csvFile);
      })
      .catch(error => {
        csvFile.status = CsvFileStatus.FAILED;
        console.log(`Failed to load file ${filename} from ${directory}`)
        console.warn(error);
      });

    return file$;
  }

  private parseCsv(text: string) {
    const lines = text.split('\n');
    const content = [];
    lines.forEach(line => {
      if (line.trim().length != 0) { // ignore empty lines
        content.push(line.split(';'));
      }
    })
    return content;
  }
}

export class LogChart {
  static overrides: ChartOverrides = {
    enableEntryLimit: false,
    entryLimit: 50
  };

  definition$: BehaviorSubject<LogChartDefinition>;
  data$: Observable<ChartDataSet[]>;
  filename: string;

  constructor(csvFileController: CsvFileController, id?: string) {
    this.filename = id ?? LogChart.generateUniqueId();
    this.definition$ = new BehaviorSubject(new LogChartDefinition());

    this.data$ = this.definition$.pipe(
      switchMap(definition => this.getDataObservable(csvFileController, definition))
    )
  }

  private getDataObservable(controller: CsvFileController, definition: LogChartDefinition): Observable<ChartDataSet[]> {

    const files$ = controller.stages$.pipe(
      switchMap(stages => {
        let stageIds = stages.map(stage => stage.id);
        let file$s = stageIds.map(stageId => controller.getFileSubject(stageId, definition.file))
        return combineLatest(file$s);
      }),
    )

    return files$.pipe(
      map(files => files.map(
        file => LogChartDefinition.getDataSet(definition, file.content, LogChart.overrides)
      ))
    );
  }

  private static generateUniqueId() {
    return `${Date.now().toString().slice(5)}${Math.random().toString().slice(2)}.chart`;
  }

  getDisplaySettingsObservable() {
    return this.definition$.pipe(
      map(definition => definition.displaySettings)
    )
  }
}

export class LogChartDefinition {
  displaySettings: ChartDisplaySettings;
  file: string;
  formatter: string;
  xVariable: string;
  yVariable: string;
  entryLimit: null | number;

  constructor() {
    this.displaySettings = new ChartDisplaySettings();
    this.file = "logfile.csv";
    this.formatter = "\"$TIMESTAMP;$0;$1;$2;$3;$SOURCE;$ERROR;!;$WINSLOW_PIPELINE_ID\""
    this.xVariable = "";
    this.yVariable = "$1";
    this.entryLimit = null;
  }

  static getDataSet(chart: LogChartDefinition, csvContent: CsvFileContent, overrides: ChartOverrides): ChartDataSet {
    let entryLimit = chart.entryLimit;

    if (overrides?.enableEntryLimit) {
      entryLimit = overrides.entryLimit;
    }

    const rows = LogChartDefinition.getLatestRows(csvContent, entryLimit);
    const variableNames = chart.formatter.split(";");
    const xIndex = variableNames.findIndex(variableName => variableName == chart.xVariable);
    const yIndex = variableNames.findIndex(variableName => variableName == chart.yVariable);

    return rows.map((rowContent, rowIndex): ChartDataPoint => {
      const step = rowIndex - rows.length + 1;
      const x = xIndex == -1 ? step : Number(rowContent[xIndex]);
      const y = yIndex == -1 ? step : Number(rowContent[yIndex]);
      return [x, y];
    });
  }

  private static getLatestRows(csvContent: CsvFileContent, displayAmount: number) {
    let sectionEnd = csvContent.length;
    let sectionStart = 0;
    if (displayAmount > 0) {
      sectionStart = sectionEnd - displayAmount;
    }

    return csvContent.slice(sectionStart, sectionEnd);
  }
}

export class ChartDisplaySettings {
  name: string = "Unnamed chart";

  xAxisName: string = "x-Axis";
  xAxisMinValue: string = "";
  xAxisMaxValue: string = "";
  xAxisType: ChartAxisType = ChartAxisType.VALUE;

  yAxisName: string = "y-Axis";
  yAxisMinValue: string = "0";
  yAxisMaxValue: string = "10";
  yAxisType: ChartAxisType = ChartAxisType.VALUE;
}

// used by echarts
export enum ChartAxisType {
  VALUE = "value",
  LOG = "log",
  TIME = "time",
}

export type ChartDataSet = ChartDataPoint[];

export type ChartDataPoint = [number, number];

export interface ChartDialogData {
  chart: LogChart;
  csvFileController: CsvFileController;
  definition: LogChartDefinition;
}

export interface ChartOverrides {
  enableEntryLimit: boolean;
  entryLimit: number;
}
