import {StageInfo} from "../api/project-api.service";

export interface StageCsvInfo {
  id: string;
  stage: StageInfo;
  csvFiles: CsvFile[]
}

export interface CsvFile {
  name: string;
  content: string[][];
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

  static getDataSeries(chart: LogChartDefinition, csvFiles: CsvFile[], displaySettings: AnalysisDisplaySettings = null): ChartDataSeries {
    let entryLimit = chart.entryLimit;

    if (displaySettings?.enableEntryLimit) {
      entryLimit = displaySettings.entryLimit;
    }

    const csvFile = csvFiles.find(csvFile => csvFile.name == chart.file);
    if (!csvFile) {
      return [];
    }

    const rows = LogChartDefinition.getLatestRows(csvFile, entryLimit);
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

  private static getLatestRows(csvFile: CsvFile, displayAmount: number) {
    let sectionEnd = csvFile.content.length;
    let sectionStart = 0;
    if (displayAmount > 0) {
      sectionStart = sectionEnd - displayAmount;
    }

    return csvFile.content.slice(sectionStart, sectionEnd);
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

export type ChartDataSeries = ChartDataPoint[];

export type ChartDataPoint = [number, number];

export interface ChartDialogData {
  chartDefinition: LogChartDefinition;
  stages: StageCsvInfo[];
}

export interface AnalysisDisplaySettings {
  enableEntryLimit: boolean;
  entryLimit: number;
}
