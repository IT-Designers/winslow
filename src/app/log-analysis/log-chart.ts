export interface CsvFile {
  name: string;
  content: [number][];
}

export class LogChart {
  settings = new ChartSettings();
  file = "logfile.csv";
  formatter = "\"$TIMESTAMP;$0;$1;$2;$3;$SOURCE;$ERROR;!;$WINSLOW_PIPELINE_ID\""
  xVariable = "";
  yVariable = "$1";
  displayAmount: null | number = null;

  static getDataSeries(chart: LogChart, csvFiles: CsvFile[]): ChartDataSeries {
    const csvFile = csvFiles.find(csvFile => csvFile.name == chart.file);
    if (!csvFile) return [];

    const formatterVariables = chart.formatter.split(";");
    const xIndex = formatterVariables.findIndex(variable => variable == chart.xVariable);
    const yIndex = formatterVariables.findIndex(variable => variable == chart.yVariable);

    const chartData = [];

    let index = 0;
    if (chart.displayAmount > 0) {
      index = csvFile.content.length - chart.displayAmount;
    }
    for (index; index < csvFile.content.length; index++) {
      const line = csvFile.content[index];
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

export enum ChartAxisType {
  VALUE = "value",
  LOG = "log",
  TIME = "time",
}

export type ChartData = ChartDataSeries[];

export type ChartDataSeries = ChartDataPoint[];

export type ChartDataPoint = [number, number];

export interface ChartDialogData {
  chart: LogChart;
  csvFiles: CsvFile[];
}
