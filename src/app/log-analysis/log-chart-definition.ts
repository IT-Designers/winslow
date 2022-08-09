import {BehaviorSubject, combineLatest, Observable} from 'rxjs';
import {map, switchMap} from 'rxjs/operators';
import {CsvFileContent} from './csv-parser';
import {CsvFile, CsvFileController} from './csv-file-controller';
import {GlobalChartSettings} from '../api/local-storage.service';
import {getColor} from './colors';

export class LogChartSnapshot {

  constructor(definition: LogChartDefinition, csvFiles: CsvFile[], globalChartSettings: GlobalChartSettings) {
    this.definition = definition;
    this.csvFiles = csvFiles;
    this.formatterVariables = LogChartSnapshot.getFormatterVariables(definition, csvFiles);
    this.graphs = csvFiles.map((file, index) => ({
      data: definition.getDataSet(file.content, this.formatterVariables, globalChartSettings),
      name: file.stageId,
      color: getColor(index)
    }));
  }

  readonly definition: LogChartDefinition;
  readonly csvFiles: CsvFile[];
  readonly graphs: ChartGraph[];
  readonly formatterVariables: string[];

  private static getFormatterVariables(definition: LogChartDefinition, csvFiles: CsvFile[]): string[] {
    if (!definition.formatterFromHeaderRow) {
      return definition.customFormatter.split(';');
    }

    const variables = [];

    csvFiles.map(csvFile => csvFile.content).forEach(csvContent => {
      if (csvContent.length > 0) {
        csvContent[0].forEach(field => {
          if (!variables.includes(field)) {
            variables.push(field);
          }
        });
      }
    });

    return variables;
  }
}

export class LogChart {

  readonly snapshot$: Observable<LogChartSnapshot>;
  readonly filename: string;
  readonly definition$: BehaviorSubject<LogChartDefinition>;

  constructor(csvFileController: CsvFileController, id?: string, definition?: LogChartDefinition) {
    this.filename = id ?? LogChart.generateUniqueId();
    this.definition$ = new BehaviorSubject(definition ?? new LogChartDefinition());

    const csvFileInfo$ = this.definition$.pipe(
      switchMap(definition => csvFileController.getCsvFiles$(definition.file)),
    );

    this.snapshot$ = combineLatest([this.definition$, csvFileInfo$, csvFileController.globalChartSettings$]).pipe(
      map(([definition, csvFileInfos, globalChartSettings]) => {
        return new LogChartSnapshot(definition, csvFileInfos, globalChartSettings);
      }),
    );
  }

  private static generateUniqueId() {
    const id = `${Date.now().toString().slice(5)}${Math.random().toString().slice(2)}`;
    return `${id}.json`;
  }
}

export class LogChartDefinition {
  displaySettings: ChartDisplaySettings;
  file: string;
  formatterFromHeaderRow: boolean;
  customFormatter: string;
  xVariable: string;
  yVariable: string;
  entryLimit: null | number;

  constructor() {
    this.displaySettings = new ChartDisplaySettings();
    this.file = 'logfile.csv';
    this.formatterFromHeaderRow = true;
    this.customFormatter = '$TIMESTAMP,$0,$1,$2,$3,$SOURCE,$ERROR,$WINSLOW_PIPELINE_ID';
    this.xVariable = '';
    this.yVariable = '$1';
    this.entryLimit = null;
  }

  getDataSet(csvContent: CsvFileContent, formatterVariables: string[], globalChartSettings: GlobalChartSettings): ChartDataSet {
    const rowLimit = globalChartSettings?.enableEntryLimit ? globalChartSettings.entryLimit : this.entryLimit;
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
  name: string = 'Unnamed chart';

  xAxisName: string = 'x-Axis';
  xAxisMinValue: number = null;
  xAxisMaxValue: number = null;
  xAxisType: ChartAxisType = ChartAxisType.VALUE;

  yAxisName: string = 'y-Axis';
  yAxisMinValue: number = null;
  yAxisMaxValue: number = null;
  yAxisType: ChartAxisType = ChartAxisType.VALUE;
}

// used by echarts
export enum ChartAxisType {
  VALUE = 'value',
  LOG = 'log',
  TIME = 'time',
}

export type ChartDataSet = ChartDataPoint[];

export type ChartDataPoint = [number, number];

export interface ChartDialogData {
  chart: LogChart;
  csvFileController: CsvFileController;
  definition: LogChartDefinition;
}

export interface ChartGraph {
  data: ChartDataSet
  name: string
  color: string
}
