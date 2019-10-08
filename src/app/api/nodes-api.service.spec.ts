import { TestBed } from '@angular/core/testing';

import { NodesApiService } from './nodes-api.service';

describe('NodesApiService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: NodesApiService = TestBed.get(NodesApiService);
    expect(service).toBeTruthy();
  });
});
