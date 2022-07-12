import {FilesApiService} from "../api/files-api.service";
import {combineLatest, interval, Observable, of} from "rxjs";
import {CsvFileContent, parseCsv} from "./csv-parser";
import {map, switchMap} from "rxjs/operators";
import {StageInfo} from "../api/project-api.service";
import {ChartOverrides} from "./log-chart-definition";

export interface CsvFile {
  stageId: string
  filename: string
  directory: string
  content: CsvFileContent
}

export class CsvFileController {
  static readonly PATH_TO_WORKSPACES = '/workspaces'

  private readonly filesApi: FilesApiService

  stages$: Observable<StageCsvInfo[]>
  overrides$: Observable<ChartOverrides>

  constructor(api, stages$, overrides$) {
    this.filesApi = api
    this.stages$ = stages$
    this.overrides$ = overrides$
  }

  getCsvFiles$(filename: string): Observable<CsvFile[]> {
    return this.stages$.pipe(
      switchMap(stages => {
        const file$s = stages.map(stage => this.getCsvFile$(stage, filename))
        return combineLatest(file$s)
      }),
    )
  }

  private static pathToCsvFilesOfStage(stageCsvInfo: StageCsvInfo) {
    return `${CsvFileController.PATH_TO_WORKSPACES}/${stageCsvInfo.stage.workspace}/.log_parser_output`
  }

  private getCsvFile$(stage: StageCsvInfo, filename: string): Observable<CsvFile> {
    const csvFileSource = this.getCsvFileSource(stage, filename)
    return csvFileSource.content$.pipe(
      map((content: CsvFileContent): CsvFile => ({
        content: content,
        directory: csvFileSource.directory,
        filename: csvFileSource.filename,
        stageId: stage.id
      }))
    )
  }

  private getCsvFileSource(stage: StageCsvInfo, filename: string): CsvFileSource {
    let csvFileSource = stage.csvFiles.find(csvFile => csvFile.filename == filename)
    if (csvFileSource == null) {
      csvFileSource = this.createCsvFileSource(stage, filename)
      stage.csvFiles.push(csvFileSource)
    }
    return csvFileSource
  }

  private content$(filepath: string): Observable<CsvFileContent> {
    return this.overrides$.pipe(
      switchMap(overrides => {
        if (overrides.enableRefreshing == false) return of(1)
        return interval(overrides.refreshTime)
      }),
      switchMap(ignored => {
        console.log(`Loading file ${filepath}.`)
        return this.filesApi.getFile(filepath)
      }),
      map(text => {
        if (text.trim().length == 0) console.warn(`File ${filepath} is empty or might be missing.`);
        return parseCsv(text)
      })
    )
  }

  private createCsvFileSource(stageCsvInfo: StageCsvInfo, filename: string): CsvFileSource {
    const directory = CsvFileController.pathToCsvFilesOfStage(stageCsvInfo);
    const filepath = `${directory}/${filename}`;

    return {
      content$: this.content$(filepath),
      directory: directory,
      filename: filename,
    };
  }

  private async getFileInfo(directory: string, filename: string) {
    const files = await this.filesApi.listFiles(directory)
    return files.find(file => file.name == filename)
  }
}

export interface StageCsvInfo {
  id: string;
  stage: StageInfo;
  csvFiles: CsvFileSource[];
}

export interface CsvFileSource {
  filename: string;
  directory: string;
  content$: Observable<CsvFileContent>;
}
