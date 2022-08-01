import {FilesApiService} from "../api/files-api.service";
import {combineLatest, Observable, of, timer} from "rxjs";
import {CsvFileContent, parseCsv} from "./csv-parser";
import {map, shareReplay, switchMap} from "rxjs/operators";
import {StageInfo} from "../api/project-api.service";
import {GlobalChartSettings} from "../api/local-storage.service";

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
  globalChartSettings$: Observable<GlobalChartSettings>

  constructor(api, stages$, globalChartSettings$) {
    this.filesApi = api
    this.stages$ = stages$
    this.globalChartSettings$ = globalChartSettings$
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
    return this.globalChartSettings$.pipe(
      switchMap(globalSettings => {
        if (globalSettings.enableRefreshing == false) return of(1) // Emit an arbitrary value once, so that the file is loaded at least once.
        const millis = globalSettings.refreshTimerInSeconds * 1000
        return timer(0, millis) // Emit a value periodically to trigger the reloading of the file.
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
      shareReplay({ // For sharing the same file source across multiple observers without having to load the same file multiple times.
        bufferSize: 1, // Replay the latest emission to new subscribers, so they do not have to wait for the file to be refreshed.
        refCount: true // Stop files from being loaded when all observers have unsubscribed.
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
