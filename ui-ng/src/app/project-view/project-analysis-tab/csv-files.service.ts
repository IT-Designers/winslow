import {Injectable} from '@angular/core';
import {FilesApiService} from '../../api/files-api.service';
import {BehaviorSubject, combineLatest, from, Observable, of, timer} from 'rxjs';
import {GlobalChartSettings, LocalStorageService} from '../../api/local-storage.service';
import {finalize, map, shareReplay, switchMap} from 'rxjs/operators';
import {CsvFileContent, parseCsv} from './csv-parser';
import {FileInfo, StageInfo} from '../../api/winslow-api';

export interface CsvFile {
  stageId: string;
  pathInWorkspace: string;
  pathToWorkspace: string;
  content: CsvFileContent;
}

interface CsvFileSource {
  pathInWorkspace: string;
  pathToWorkspace: string;
  content$: Observable<CsvFileContent>;
}

@Injectable({
  providedIn: 'root'
})
export class CsvFilesService {

  private static readonly WORKSPACE_DIR = '/workspaces';

  private readonly stages$: BehaviorSubject<StageInfo[]>;
  readonly globalChartSettings$: BehaviorSubject<GlobalChartSettings>;

  constructor(
    private filesApi: FilesApiService,
    localStorageService: LocalStorageService,
  ) {
    const settings = localStorageService.getChartSettings();
    this.globalChartSettings$ = new BehaviorSubject<GlobalChartSettings>(settings);

    this.stages$ = new BehaviorSubject<StageInfo[]>([]);
  }

  private csvFileSources: CsvFileSource[] = [];

  setStages(stages: StageInfo[]) {
    this.stages$.next(stages);
  }

  getCsvFiles$(filepath: string): Observable<CsvFile[]> {
    return this.stages$.pipe(
      switchMap(stages =>
        combineLatest(stages
            .filter(stage => stage.workspace != undefined)
            .map(stage => this.getCsvFile$(<string>stage.workspace, stage.id, filepath))
        )),
    );
  }

  getFileSuggestions$(path$: Observable<string>): Observable<string[]> {
    return combineLatest([this.stages$, path$]).pipe(
      switchMap(([stages, path]) => from(this.getFileSuggestions(stages, path))),
    )
  }

  private async getFileSuggestions(stages: StageInfo[], currentPath: string): Promise<string[]> {
    const combined_suggestions: string[] = []
    const workspace_suggestion_lists = (await Promise.all(stages.map(stage => {
      if (stage.workspace == undefined) {
        return [];
      }
      return this.getFileSuggestionsForWorkspace(stage.workspace, currentPath);
    })))
    for (const workspace_suggestions of workspace_suggestion_lists) {
      for (const workspace_suggestion of workspace_suggestions) {
        if (!combined_suggestions.includes(workspace_suggestion)) {
          combined_suggestions.push(workspace_suggestion);
        }
      }
    }
    combined_suggestions.sort();
    return combined_suggestions;
  }

  private async getFileSuggestionsForWorkspace(pathToWorkspace: string, currentPath: string): Promise<string[]> {
    const api = this.filesApi;
    const topLevelPath = `${CsvFilesService.WORKSPACE_DIR}/${pathToWorkspace}/`
    const paths: string[] = []

    const topLevelFileInfos = await api.listFiles(topLevelPath);

    async function walk(fileInfos: FileInfo[]) {
      for (const fileInfo of fileInfos) {
        const relativePath = fileInfo.path.substring(topLevelPath.length);
        if (fileInfo.directory) {
          await walk(await api.listFiles(fileInfo.path));
          continue;
        }
        if (!relativePath.startsWith(currentPath)) {
          continue;
        }
        if (fileInfo.name.toLowerCase().endsWith('.csv')) {
          paths.push(relativePath);
        }
      }
    }

    await walk(topLevelFileInfos);

    return paths;
  }

  private getCsvFile$(workspace: string, stageId: string, filepath: string): Observable<CsvFile> {
    const csvFileSource = this.getCsvFileSource(workspace, filepath);
    return csvFileSource.content$.pipe(
      map((content: CsvFileContent): CsvFile => ({
        content: content,
        pathToWorkspace: csvFileSource.pathToWorkspace,
        pathInWorkspace: csvFileSource.pathInWorkspace,
        stageId: stageId,
      }))
    );
  }

  private getCsvFileSource(stageWorkspace: string, filepath: string): CsvFileSource {
    let csvFileSource = this.csvFileSources
      .find(csvFile => csvFile.pathInWorkspace == filepath && csvFile.pathToWorkspace == stageWorkspace);

    if (csvFileSource == null) {
      csvFileSource = this.createCsvFileSource(stageWorkspace, filepath);
    }

    return csvFileSource;
  }

  private createCsvFileSource(stageWorkspace: string, relativePathToFile: string): CsvFileSource {
    const fullPathToFile = CsvFilesService.fullPathToFile(stageWorkspace, relativePathToFile);

    const content$ = this.globalChartSettings$.pipe(
      switchMap(globalSettings => {
        console.log(`Watching file ${fullPathToFile}.`);
        if (!globalSettings.enableRefreshing) return of(1); // Emit an arbitrary value once, so that the file is loaded at least once.
        const millis = globalSettings.refreshTimerInSeconds * 1000;
        return timer(0, millis); // Emit a value periodically to trigger the reloading of the file.
      }),
      switchMap(_ => {
        console.log(`Loading file ${fullPathToFile}.`);
        return this.filesApi.getFile(fullPathToFile);
      }),
      map(text => {
        console.log(`Finished loading file ${fullPathToFile}.`);
        return parseCsv(text);
      }),
      finalize(() => {
        console.log(`Finished watching file ${fullPathToFile}.`);
        this.removeCsvFileSource(csvFileSource)
      }),
      shareReplay({ // For sharing the same file source across multiple observers without having to load the same file multiple times.
        bufferSize: 1, // Replay the latest emission to new subscribers, so they do not have to wait for the file to be refreshed.
        refCount: true // Stop files from being loaded when all observers have unsubscribed.
      }),
    );

    const csvFileSource: CsvFileSource = {
      content$: content$,
      pathToWorkspace: stageWorkspace,
      pathInWorkspace: relativePathToFile,
    }

    this.csvFileSources.push(csvFileSource);
    return csvFileSource;
  }

  private removeCsvFileSource(csvFileSource: CsvFileSource) {
    const index = this.csvFileSources.indexOf(csvFileSource)
    this.csvFileSources.splice(index, 1)
  }

  private static fullPathToFile(pathToWorkspace: string, pathInWorkSpace: string) {
    return `${CsvFilesService.WORKSPACE_DIR}/${pathToWorkspace}/${pathInWorkSpace}`
  }
}
