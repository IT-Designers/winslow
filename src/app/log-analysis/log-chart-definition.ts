import {BehaviorSubject, combineLatest, Observable} from "rxjs";
import {map, switchMap} from "rxjs/operators";
import {CsvFileContent} from "./csv-parser";
import {CsvFileController} from "./csv-file-controller";

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
