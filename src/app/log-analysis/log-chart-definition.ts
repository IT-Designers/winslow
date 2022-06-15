import {StageInfo} from "../api/project-api.service";
import {BehaviorSubject, combineLatest, Observable, Subject} from "rxjs";
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
  content$: Subject<CsvFileContent>;
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
          const csvFileInfo = this.selectCsvFile(stage, filename);
          return csvFileInfo.content$;
        })
        return combineLatest(content$s);
      }),
    )
  }

  private selectCsvFile(stage: StageCsvInfo, filename: string): CsvFileInfo {
    let csvFile = stage.csvFiles.find(csvFile => csvFile.filename == filename)
    if (csvFile == null) {
      csvFile = this.loadCsvFile(stage, filename);
      stage.csvFiles.push(csvFile);
    }
    return csvFile;
  }

  private loadCsvFile(stageCsvInfo: StageCsvInfo, filename: string): CsvFileInfo {
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

export class LogChartSnapshot {

  constructor(definition: LogChartDefinition, csvFileContents: CsvFileContent[]) {
    this.definition = definition
    this.csvFileContents = csvFileContents
    this.formatterVariables = LogChartSnapshot.getFormatterVariables(definition, csvFileContents)
    this.chartData = LogChartSnapshot.getChartData(definition, csvFileContents, this.formatterVariables)
  }

  readonly definition: LogChartDefinition
  readonly csvFileContents: CsvFileContent[]
  readonly chartData: ChartDataSet[]
  readonly formatterVariables: string[]

  private static getFormatterVariables(definition: LogChartDefinition, csvFileContents: CsvFileContent[]): string[] {
    if (!definition.formatterFromHeaderRow) return definition.customFormatter.split(";");

    const variables = [];

    csvFileContents.forEach(csvFile => {
      if (csvFile.length <= 0) return;
      csvFile[0].forEach(field => {
        if (variables.includes(field)) return
        variables.push(field)
      })
    })

    return variables
  }

  private static getChartData(definition: LogChartDefinition, csvFileContents: CsvFileContent[], formatterVariables: string[]) {
    return csvFileContents.map(
      csvFileContent => definition.getDataSet(csvFileContent, formatterVariables)
    )
  }
}

export class LogChart {
  static overrides: ChartOverrides = {
    enableEntryLimit: false,
    entryLimit: 50
  };

  readonly snapshot$: Observable<LogChartSnapshot>
  readonly filename: string;
  readonly definition$: BehaviorSubject<LogChartDefinition>;

  constructor(csvFileController: CsvFileController, id?: string, definition?: LogChartDefinition) {
    this.filename = id ?? LogChart.generateUniqueId();
    this.definition$ = new BehaviorSubject(definition ?? new LogChartDefinition());

    let csvFileContents$ = this.definition$.pipe(
      switchMap(definition => csvFileController.getCsvContents$(definition.file).pipe()),
    )

    this.snapshot$ = combineLatest([this.definition$, csvFileContents$]).pipe(
      map(([definition, csvFileContents]) => new LogChartSnapshot(definition, csvFileContents))
    )
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
    this.customFormatter = "$TIMESTAMP,$0,$1,$2,$3,$SOURCE,$ERROR,$WINSLOW_PIPELINE_ID"
    this.xVariable = ""
    this.yVariable = "$1"
    this.entryLimit = null
  }

  getDataSet(csvContent: CsvFileContent, formatterVariables: string[]): ChartDataSet {
    const overrides = LogChart.overrides
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
