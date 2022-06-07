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

  getFileContentsObservable(filename: string) {

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
    const contentsObservable = controller.getFileContentsObservable(definition.file);
    return contentsObservable.pipe(
      map(csvFileContents => csvFileContents.map(csvFileContent => {
        return LogChartDefinition.getDataSet(definition, csvFileContent, LogChart.overrides)
      }))
    )
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
  displaySettings: ChartDisplaySettings
  file: string
  formatterFromHeaderRow: boolean
  formatter: string
  xVariable: string
  yVariable: string
  entryLimit: null | number

  constructor() {
    this.displaySettings = new ChartDisplaySettings()
    this.file = "logfile.csv"
    this.formatterFromHeaderRow = true
    this.formatter = "$TIMESTAMP;$0;$1;$2;$3;$SOURCE;$ERROR;!;$WINSLOW_PIPELINE_ID"
    this.xVariable = ""
    this.yVariable = "$1"
    this.entryLimit = null
  }

  static getDataSet(chart: LogChartDefinition, csvContent: CsvFileContent, overrides: ChartOverrides): ChartDataSet {
    if (csvContent.length == 0) {
      console.warn(`File ${chart.file} for chart ${chart.displaySettings.name} appears to be empty.`)
      return []
    }

    const rowLimit = overrides?.enableEntryLimit ? overrides.entryLimit : chart.entryLimit;
    const rows = LogChartDefinition.getLatestRows(csvContent, rowLimit);
    const variableNames = chart.formatterFromHeaderRow ? csvContent[0] : chart.formatter.split(";");
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
  yAxisMinValue: string = "";
  yAxisMaxValue: string = "";
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
