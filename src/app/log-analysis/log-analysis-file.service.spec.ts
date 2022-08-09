import { TestBed } from '@angular/core/testing';

import { CsvFilesService } from './csv-files.service';

describe('LogAnalysisFileService', () => {
  let service: CsvFilesService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CsvFilesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
