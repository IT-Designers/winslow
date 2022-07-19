import {FilesApiService} from "../api/files-api.service";
import {combineLatest, Observable, of, timer} from "rxjs";
import {CsvFileContent, parseCsv} from "./csv-parser";
import {map, shareReplay, switchMap} from "rxjs/operators";
import {StageInfo} from "../api/project-api.service";
import {ChartOverrides} from "./log-chart-definition";

export interface CsvFile {
  stageId: string
  pathInWorkspace: string
  pathToWorkspace: string
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

  getCsvFiles$(filepath: string): Observable<CsvFile[]> {
    return this.stages$.pipe(
      switchMap(stages => {
        const file$s = stages.map(stage => this.getCsvFile$(stage, filepath))
        return combineLatest(file$s)
      }),
    )
  }

  private getCsvFile$(stage: StageCsvInfo, filepath: string): Observable<CsvFile> {
    const csvFileSource = this.getCsvFileSource(stage, filepath)
    return csvFileSource.content$.pipe(
      map((content: CsvFileContent): CsvFile => ({
        content: content,
        pathToWorkspace: csvFileSource.pathToWorkspace,
        pathInWorkspace: csvFileSource.pathInWorkspace,
        stageId: stage.id
      }))
    )
  }

  private getCsvFileSource(stage: StageCsvInfo, filepath: string): CsvFileSource {
    let csvFileSource = stage.csvFiles.find(csvFile => csvFile.pathInWorkspace == filepath)
    if (csvFileSource == null) {
      csvFileSource = this.createCsvFileSource(stage, filepath)
      stage.csvFiles.push(csvFileSource)
    }
    return csvFileSource
  }

  private content$(fullPathToFile: string): Observable<CsvFileContent> {
    return this.overrides$.pipe(
      switchMap(overrides => {
        if (overrides.enableRefreshing == false) return of(1)
        const millis = overrides.refreshTimerInSeconds * 1000
        return timer(0, millis)
      }),
      switchMap(ignored => {
        console.log(`Loading file ${fullPathToFile}.`)
        return this.filesApi.getFile(fullPathToFile)
      }),
      map(text => {
        console.log(`Finished loading file ${fullPathToFile}.`)
        if (text.trim().length == 0) console.warn(`File ${fullPathToFile} is empty or might be missing.`);
        return parseCsv(text)
      }),
      shareReplay({
        bufferSize: 1,
        refCount: true
      })
    )
  }

  private createCsvFileSource(stageCsvInfo: StageCsvInfo, relativePathToFile: string): CsvFileSource {
    const fullPathToWorkspace = `${CsvFileController.PATH_TO_WORKSPACES}/${stageCsvInfo.stage.workspace}/.log_parser_output`
    const fullPathToFile = `${fullPathToWorkspace}/${relativePathToFile}`;

    console.log(`Watching file ${fullPathToFile}.`)

    return {
      content$: this.content$(fullPathToFile),
      pathToWorkspace: fullPathToWorkspace,
      pathInWorkspace: relativePathToFile,
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
  pathInWorkspace: string;
  pathToWorkspace: string;
  content$: Observable<CsvFileContent>;
}
