import { TestBed } from '@angular/core/testing';

import { PipelineApiService } from './pipeline-api.service';

describe('PipelineApiService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: PipelineApiService = TestBed.get(PipelineApiService);
    expect(service).toBeTruthy();
  });
});
