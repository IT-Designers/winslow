import {FilesApiService} from "../api/files-api.service";
import {BehaviorSubject, combineLatest, Observable, Subject} from "rxjs";
import {CsvFileContent, parseCsv} from "./csv-parser";
import {switchMap} from "rxjs/operators";
import {StageInfo} from "../api/project-api.service";

export class CsvFileController {
  static readonly PATH_TO_WORKSPACES = '/workspaces';

  private readonly filesApi: FilesApiService;

  stages$: BehaviorSubject<StageCsvInfo[]>;

  constructor(api) {
    this.filesApi = api;
    this.stages$ = new BehaviorSubject<StageCsvInfo[]>([]);
  }

  getCsvContents$(filename: string): Observable<CsvFileContent[]> {
    return this.stages$.pipe(
      switchMap(stages => {
        const content$s = stages.map(stage => {
          const csvFileInfo = this.selectCsvFile(stage, filename);
          return csvFileInfo.content$;
        })
        return combineLatest(content$s);
      }),
    )
  }

  private selectCsvFile(stage: StageCsvInfo, filename: string): CsvFileSource {
    let csvFileSource = stage.csvFiles.find(csvFile => csvFile.filename == filename)
    if (csvFileSource == null) {
      csvFileSource = this.loadCsvFile(stage, filename);
      stage.csvFiles.push(csvFileSource);
    }
    return csvFileSource;
  }

  private loadCsvFile(stageCsvInfo: StageCsvInfo, filename: string): CsvFileSource {
    const directory = CsvFileController.getFileDirectory(stageCsvInfo);
    const filepath = `${directory}/${filename}`;

    const csvFileSource: CsvFileSource = {
      content$: new BehaviorSubject<CsvFileContent>([]),
      directory: directory,
      filename: filename,
    }

    console.log(`Loading file ${filename} from ${directory}`);

    this.filesApi.getFile(filepath).toPromise()
      .then(text => {
        const content = parseCsv(text);
        console.log(`Finished loading file ${filename} from ${directory}`)
        csvFileSource.content$.next(content);

        if (text.trim().length == 0) {
          console.warn(`File ${filename} from ${directory} is empty or might be missing.`);
        }

      })
      .catch(error => {
        console.log(`Failed to load file ${filename} from ${directory}`)
        console.warn(error);
      });

    return csvFileSource;
  }

  private static getFileDirectory(stageCsvInfo: StageCsvInfo) {
    return `${CsvFileController.PATH_TO_WORKSPACES}/${stageCsvInfo.stage.workspace}/.log_parser_output`;
  }
}

export interface StageCsvInfo {
  id: string;
  stage: StageInfo;
  csvFiles: CsvFileSource[];
}

interface CsvFileSource {
  filename: string;
  directory: string;
  content$: Subject<CsvFileContent>;
}
