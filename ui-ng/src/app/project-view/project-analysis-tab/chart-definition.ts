import {BehaviorSubject, combineLatest, Observable} from 'rxjs';
import {map, switchMap} from 'rxjs/operators';
import {CsvFileContent, parseCsv} from './csv-parser';
import {GlobalChartSettings} from '../../api/local-storage.service';
import {generateColor} from './colors';
import {CsvFile, CsvFilesService} from './csv-files.service';
import {ChartDefinition, ChartAxisType} from "../../api/winslow-api";


export class ChartSnapShot {

  constructor(definition: ChartDefinition, csvFiles: CsvFile[], globalChartSettings: GlobalChartSettings) {
    this.definition = definition;
    this.csvFiles = csvFiles;
    this.formatterVariables = ChartSnapShot.getFormatterVariables(definition, csvFiles);
    this.graphs = csvFiles.map((file, index) => ({
      data: ChartSnapShot.getDataSet(definition, file.content, this.formatterVariables, globalChartSettings),
      name: file.stageId,
      color: generateColor(index)
    }));
  }

  readonly definition: ChartDefinition;
  readonly csvFiles: CsvFile[];
  readonly graphs: ChartGraph[];
  readonly formatterVariables: string[];

  private static getFormatterVariables(definition: ChartDefinition, csvFiles: CsvFile[]): string[] {
    if (!definition.formatterFromHeaderRow) {
      // formatter can be treated as a CSV file with 1 row
      let parsedFormatter = parseCsv(definition.customFormatter);
      if (parsedFormatter.length > 0) {
        return parsedFormatter[0];
      } else {
        return [];
      }
    }

    const variables: string[] = [];

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

  private static getDataSet(definition: ChartDefinition, csvContent: CsvFileContent, formatterVariables: string[], globalChartSettings: GlobalChartSettings): ChartDataSet {
    const rowLimit = globalChartSettings?.enableEntryLimit ? globalChartSettings.entryLimit : definition.entryLimit;
    const rows = ChartSnapShot.getLatestRows(csvContent, rowLimit ?? 0);
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

export class AnalysisChart {

  readonly snapshot$: Observable<ChartSnapShot>;
  readonly definition$: BehaviorSubject<ChartDefinition>;

  constructor(service: CsvFilesService, definition?: ChartDefinition) {
    this.definition$ = new BehaviorSubject(definition ?? defaultChartDefinition());

    const csvFileInfos$ = this.definition$.pipe(
      switchMap(definition => service.getCsvFiles$(definition.file)),
    );

    this.snapshot$ = combineLatest([this.definition$, csvFileInfos$, service.globalChartSettings$]).pipe(
      map(([definition, csvFileInfos, globalChartSettings]) => {
        return new ChartSnapShot(definition, csvFileInfos, globalChartSettings);
      }),
    );
  }
}

  function defaultChartDefinition() {
    return new ChartDefinition({
      customFormatter: '$TIMESTAMP,$0,$1,$2,$3,$SOURCE,$ERROR,$WINSLOW_PIPELINE_ID',
      entryLimit: undefined,
      file: '.log_parser_output/example.csv',
      formatterFromHeaderRow: true,
      name: 'Unnamed Chart',
      xAxisMaxValue: undefined,
      xAxisMinValue: undefined,
      xAxisName: "x-Axis",
      xAxisType: 'VALUE',
      xVariable: "",
      yAxisMaxValue: 0,
      yAxisMinValue: 100,
      yAxisName: "y-Axis",
      yAxisType: 'VALUE',
      yVariable: "$1"
    });
  }


export type ChartDataSet = ChartDataPoint[];

export type ChartDataPoint = [number, number];

export interface ChartGraph {
  data: ChartDataSet;
  name: string;
  color: string;
}
