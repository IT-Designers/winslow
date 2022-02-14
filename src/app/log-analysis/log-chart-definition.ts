import {ExecutionGroupInfo} from "../api/project-api.service";

export interface StageCsvInfo {
  id: string;
  executionGroup: ExecutionGroupInfo;
  csvFiles: CsvFile[]
}

export interface CsvFile {
  name: string;
  content: string[][];
}

export class LogChartDefinition {
  settings: ChartSettings;
  file: string;
  formatter: string;
  xVariable: string;
  yVariable: string;
  displayAmount: null | number;

  constructor() {
    this.settings = new ChartSettings();
    this.file = "logfile.csv";
    this.formatter = "\"$TIMESTAMP;$0;$1;$2;$3;$SOURCE;$ERROR;!;$WINSLOW_PIPELINE_ID\""
    this.xVariable = "";
    this.yVariable = "$1";
    this.displayAmount = null;
  }

  static getDataSeries(chart: LogChartDefinition, csvFiles: CsvFile[]): ChartDataSeries {
    const csvFile = csvFiles.find(csvFile => csvFile.name == chart.file);
    if (!csvFile) return [];

    const formatterVariables = chart.formatter.split(";");
    const xIndex = formatterVariables.findIndex(variable => variable == chart.xVariable);
    const yIndex = formatterVariables.findIndex(variable => variable == chart.yVariable);

    const chartData = [];

    let limit = csvFile.content.length;
    let offset = 0;
    if (chart.displayAmount > 0) {
      offset = csvFile.content.length - chart.displayAmount;
      limit = chart.displayAmount;
    }
    for (let index = 0; index < limit; index++) {
      const line = csvFile.content[index + offset];
      chartData.push([line[xIndex] ?? index, line[yIndex] ?? index])
    }

    return chartData;
  }
}

export class ChartSettings {
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
