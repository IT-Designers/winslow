import {StageInfo} from "../api/project-api.service";
import {BehaviorSubject, combineLatest, Observable} from "rxjs";
import {FilesApiService} from "../api/files-api.service";
import {map, switchMap} from "rxjs/operators";
import {CsvFileContent, parseCsv} from "./csv-parser";

export interface StageCsvInfo {
  id: string;
  stage: StageInfo;
  csvFiles: CsvFileInfo[];
}

export interface CsvFileInfo {
  filename: string;
  directory: string;
  content$: BehaviorSubject<CsvFileContent>;
}

export class CsvFileController {
  static readonly PATH_TO_WORKSPACES = '/workspaces';

  private readonly filesApi: FilesApiService;

  stages$: BehaviorSubject<StageCsvInfo[]>;

  constructor(api) {
    this.filesApi = api;
    this.stages$ = new BehaviorSubject<StageCsvInfo[]>([]);
  }

  getCsvContents$(filename: string): Observable<CsvFileContent[]> {
    return this.stages$.pipe(
      switchMap(stages => {
        const content$s = stages.map(stage => {
          const csvFile = this.getCsvFileInfo(stage, filename);
          return csvFile.content$;
        })
        return combineLatest(content$s);
      }),
    )
  }

  private getCsvFileInfo(stage: StageCsvInfo, filename: string) {
    let csvFile = stage.csvFiles.find(csvFile => csvFile.filename == filename)
    if (csvFile == null) {
      csvFile = this.loadCsvFile(stage, filename);
      stage.csvFiles.push(csvFile);
    }
    return csvFile;
  }

  private loadCsvFile(stageCsvInfo: StageCsvInfo, filename: string) {
    const directory = CsvFileController.getFileDirectory(stageCsvInfo);
    const filepath = `${directory}/${filename}`;

    const csvFile: CsvFileInfo = {
      content$: new BehaviorSubject<CsvFileContent>([]),
      directory: directory,
      filename: filename,
    }

    console.log(`Loading file ${filename} from ${directory}`);

    this.filesApi.getFile(filepath).toPromise()
      .then(text => {
        const content = parseCsv(text);
        console.log(`Finished loading file ${filename} from ${directory}`)
        csvFile.content$.next(content);

        if (text.trim().length == 0) {
          console.warn(`File ${filename} from ${directory} is empty or might be missing.`);
        }

      })
      .catch(error => {
        console.log(`Failed to load file ${filename} from ${directory}`)
        console.warn(error);
      });

    return csvFile;
  }

  private static getFileDirectory(stageCsvInfo: StageCsvInfo) {
    return `${CsvFileController.PATH_TO_WORKSPACES}/${stageCsvInfo.stage.workspace}/.log_parser_output`;
  }
}

export class LogChart {
  static overrides: ChartOverrides = {
    enableEntryLimit: false,
    entryLimit: 50
  };

  readonly filename: string;
  readonly definition$: BehaviorSubject<LogChartDefinition>;
  readonly displaySettings$: Observable<ChartDisplaySettings>;
  readonly csvFileContents$: Observable<CsvFileContent[]>
  readonly chartData$: Observable<ChartDataSet[]>;
  readonly formatterVariables$: Observable<string[]>;

  constructor(csvFileController: CsvFileController, id?: string) {
    this.filename = id ?? LogChart.generateUniqueId();
    this.definition$ = new BehaviorSubject(new LogChartDefinition());

    this.displaySettings$ = this.definition$.pipe(
      map(definition => definition.displaySettings)
    )

    this.csvFileContents$ = this.definition$.pipe(
      switchMap(definition => csvFileController.getCsvContents$(definition.file))
    )

    this.formatterVariables$ = this.definition$.pipe(
      switchMap(definition => this.csvFileContents$.pipe(
        map(csvFileContents => LogChart.getFormatterVariables(definition, csvFileContents[0] ?? [])),
      ))
    )

    this.chartData$ = combineLatest([this.definition$, this.csvFileContents$, this.formatterVariables$]).pipe(
      map(([definition, csvFileContents, formatterVariables]) => csvFileContents.map(
        csvFileContent => definition.getDataSet(csvFileContent, formatterVariables, LogChart.overrides))
      )
    )
  }

  private static getFormatterVariables(definition: LogChartDefinition, csvContent: string[][]) {
    if (definition.formatterFromHeaderRow) {
      if (csvContent.length == 0) {
        return []
      }
      return csvContent[0];
    } else {
      return definition.customFormatter.split(";");
    }
  }

  private static generateUniqueId() {
    const id = `${Date.now().toString().slice(5)}${Math.random().toString().slice(2)}`
    return `${id}.json`;
  }
}

export class LogChartDefinition {
  displaySettings: ChartDisplaySettings
  file: string
  formatterFromHeaderRow: boolean
  customFormatter: string
  xVariable: string
  yVariable: string
  entryLimit: null | number

  constructor() {
    this.displaySettings = new ChartDisplaySettings()
    this.file = "logfile.csv"
    this.formatterFromHeaderRow = true
    this.customFormatter = "$TIMESTAMP;$0;$1;$2;$3;$SOURCE;$ERROR;!;$WINSLOW_PIPELINE_ID"
    this.xVariable = ""
    this.yVariable = "$1"
    this.entryLimit = null
  }

  getDataSet(csvContent: CsvFileContent, formatterVariables: string[], overrides: ChartOverrides): ChartDataSet {
    const rowLimit = overrides?.enableEntryLimit ? overrides.entryLimit : this.entryLimit;
    const rows = LogChartDefinition.getLatestRows(csvContent, rowLimit);
    const xIndex = formatterVariables.findIndex(variableName => variableName == this.xVariable);
    const yIndex = formatterVariables.findIndex(variableName => variableName == this.yVariable);

    return rows.map((rowContent, rowIndex): ChartDataPoint => {
      const relativeRowIndex = rowIndex - rows.length + 1;
      const x = xIndex == -1 ? relativeRowIndex : Number(rowContent[xIndex]);
      const y = yIndex == -1 ? relativeRowIndex : Number(rowContent[yIndex]);
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
  xAxisMinValue: number = null;
  xAxisMaxValue: number = null;
  xAxisType: ChartAxisType = ChartAxisType.VALUE;

  yAxisName: string = "y-Axis";
  yAxisMinValue: number = null
  yAxisMaxValue: number = null;
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
