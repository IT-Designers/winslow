import {StageInfo} from "../api/project-api.service";
import {BehaviorSubject} from "rxjs";
import {FilesApiService} from "../api/files-api.service";

export interface StageCsvInfo {
  id: string;
  stage: StageInfo;
  csvFiles: CsvFileInfo[]
}

export enum CsvFileStatus {
  NOT_LOADED,
  LOADING,
  OK,
  FILE_IS_EMPTY_OR_MISSING,
  FAILED,
}

export class CsvFileInfo {
  filename: string;
  directory: string;
  status: CsvFileStatus;
  content: CsvFileContent;

  constructor(directory: string, filename: string) {
    this.filename = filename;
    this.directory = directory;
    this.status = CsvFileStatus.NOT_LOADED;
  }

  loadFrom(filesApi: FilesApiService) {
    this.status = CsvFileStatus.LOADING;

    console.log(`Loading file ${this.filename} from ${this.directory}`);

    const filepath = `${this.directory}/${this.filename}`;

    return filesApi.getFile(filepath).toPromise()
      .then(text => {
        this.content = this.parse(text);
        this.status = CsvFileStatus.OK;
        console.log(`Finished loading file ${this.filename} from ${this.directory}`)

        if (text.trim().length == 0) {
          this.status = CsvFileStatus.FILE_IS_EMPTY_OR_MISSING;
          console.warn(`File ${this.filename} from ${this.directory} is empty or might be missing.`);
        }
      })
      .catch(error => {
        this.status = CsvFileStatus.FAILED;
        console.log(`Failed to load file ${this.filename} from ${this.directory}`)
        console.warn(error);
      });
  }

  private parse(text: string) {
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

export type CsvFileContent = string[][]

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

  static getDataSeries(chart: LogChartDefinition, csvContent: CsvFileContent, displaySettings: AnalysisDisplaySettings = null): ChartDataSet {
    let entryLimit = chart.entryLimit;

    if (displaySettings?.enableEntryLimit) {
      entryLimit = displaySettings.entryLimit;
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
  chartDefinition: LogChartDefinition;
  dataSource: BehaviorSubject<ChartDataSet[]>
  stages: StageCsvInfo[];
}

export interface AnalysisDisplaySettings {
  enableEntryLimit: boolean;
  entryLimit: number;
}
