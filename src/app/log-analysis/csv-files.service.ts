import {Injectable} from '@angular/core';
import {FilesApiService} from '../api/files-api.service';
import {BehaviorSubject, combineLatest, Observable, of, timer} from 'rxjs';
import {StageInfo} from '../api/project-api.service';
import {GlobalChartSettings, LocalStorageService} from '../api/local-storage.service';
import {map, shareReplay, switchMap} from 'rxjs/operators';
import {CsvFileContent, parseCsv} from './csv-parser';

export interface CsvFile {
  stageId: string;
  pathInWorkspace: string;
  pathToWorkspace: string;
  content: CsvFileContent;
}

export interface CsvFileSource {
  pathInWorkspace: string;
  pathToWorkspace: string;
  content$: Observable<CsvFileContent>;
}

@Injectable({
  providedIn: 'root'
})
export class CsvFilesService {

  private static readonly WORKSPACE_DIR = '/workspaces'
  private static readonly CSV_FILE_DIR = '.log_parser_output';

  private readonly stages$: BehaviorSubject<StageInfo[]>
  readonly globalChartSettings$: BehaviorSubject<GlobalChartSettings>

  constructor(
    private filesApi: FilesApiService,
    private localStorageService: LocalStorageService,
  ) {
    const settings = localStorageService.getChartSettings()
    this.globalChartSettings$ = new BehaviorSubject<GlobalChartSettings>(settings)

    this.stages$ = new BehaviorSubject<StageInfo[]>([])
  }

  private csvFileSources: CsvFileSource[] = [];

  setStages(stages: StageInfo[]) {
    this.removeObsoleteFileSources(stages)
    this.stages$.next(stages)
  }

  getCsvFiles$(filepath: string): Observable<CsvFile[]> {
    return this.stages$.pipe(
      switchMap(stages => {
        const file$s = stages.map(stage => this.getCsvFile$(stage, filepath))
        return combineLatest(file$s)
      }),
    )
  }

  private getCsvFile$(stage: StageInfo, filepath: string): Observable<CsvFile> {
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

  private getCsvFileSource(stage: StageInfo, filepath: string): CsvFileSource {
    let csvFileSource = this.csvFileSources.find(csvFile => csvFile.pathInWorkspace == filepath && csvFile.pathToWorkspace == stage.workspace)
    if (csvFileSource == null) {
      csvFileSource = this.createCsvFileSource(stage, filepath)
      this.csvFileSources.push(csvFileSource)
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

  private createCsvFileSource(stage: StageInfo, relativePathToFile: string): CsvFileSource {
    const fullPathToFile = CsvFilesService.fullPathToFile(stage.workspace, relativePathToFile)

    console.log(`Watching file ${fullPathToFile}.`)

    return {
      content$: this.content$(fullPathToFile),
      pathToWorkspace: stage.workspace,
      pathInWorkspace: relativePathToFile,
    };
  }

  private removeObsoleteFileSources(stages: StageInfo[]) {
    const workspaces = stages.map(stage => stage.workspace)
    this.csvFileSources = this.csvFileSources.filter(source => workspaces.includes(source.pathToWorkspace))
  }

  private async getFileInfo(directory: string, filename: string) {
    const files = await this.filesApi.listFiles(directory)
    return files.find(file => file.name == filename)
  }

  private static fullPathToFile(pathToWorkspace: string, pathInWorkSpace: string) {
    return `${CsvFilesService.WORKSPACE_DIR}/${pathToWorkspace}/${CsvFilesService.CSV_FILE_DIR}/${pathInWorkSpace}`
  }
}
