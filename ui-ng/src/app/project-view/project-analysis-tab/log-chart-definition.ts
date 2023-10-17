import {BehaviorSubject, combineLatest, Observable} from 'rxjs';
import {map, switchMap} from 'rxjs/operators';
import {CsvFileContent, parseCsv} from './csv-parser';
import {GlobalChartSettings} from '../../api/local-storage.service';
import {generateColor} from './colors';
import {CsvFile, CsvFilesService} from './csv-files.service';

export class LogChartSnapshot {

  constructor(definition: LogChartDefinition, csvFiles: CsvFile[], globalChartSettings: GlobalChartSettings) {
    this.definition = definition;
    this.csvFiles = csvFiles;
    this.formatterVariables = LogChartSnapshot.getFormatterVariables(definition, csvFiles);
    this.graphs = csvFiles.map((file, index) => ({
      data: LogChartSnapshot.getDataSet(definition, file.content, this.formatterVariables, globalChartSettings),
      name: file.stageId,
      color: generateColor(index)
    }));
  }

  readonly definition: LogChartDefinition;
  readonly csvFiles: CsvFile[];
  readonly graphs: ChartGraph[];
  readonly formatterVariables: string[];

  private static getFormatterVariables(definition: LogChartDefinition, csvFiles: CsvFile[]): string[] {
    if (!definition.formatterFromHeaderRow) {
      // formatter can be treated as a CSV file with 1 row
      let parsedFormatter = parseCsv(definition.customFormatter);
      if (parsedFormatter.length > 0) {
        return parsedFormatter[0];
      } else {
        return [];
      }
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

  private static getDataSet(definition: LogChartDefinition, csvContent: CsvFileContent, formatterVariables: string[], globalChartSettings: GlobalChartSettings): ChartDataSet {
    const rowLimit = globalChartSettings?.enableEntryLimit ? globalChartSettings.entryLimit : definition.entryLimit;
    const rows = LogChartSnapshot.getLatestRows(csvContent, rowLimit);
    const xIndex = formatterVariables.findIndex(variableName => variableName == definition.xVariable);
    const yIndex = formatterVariables.findIndex(variableName => variableName == definition.yVariable);

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

export class LogChart {

  readonly snapshot$: Observable<LogChartSnapshot>;
  readonly filename: string;
  readonly definition$: BehaviorSubject<LogChartDefinition>;

  constructor(service: CsvFilesService, id?: string, definition?: LogChartDefinition) {
    this.filename = id ?? LogChart.generateUniqueId();
    this.definition$ = new BehaviorSubject(definition ?? new LogChartDefinition());

    const csvFileInfos$ = this.definition$.pipe(
      switchMap(definition => service.getCsvFiles$(definition.file)),
    );

    this.snapshot$ = combineLatest([this.definition$, csvFileInfos$, service.globalChartSettings$]).pipe(
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

export interface ChartGraph {
  data: ChartDataSet;
  name: string;
  color: string;
}
