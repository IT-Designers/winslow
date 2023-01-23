import { TestBed } from '@angular/core/testing';

import { DefaultApiServiceService } from './default-api-service.service';

describe('DefaultApiServiceService', () => {
  let service: DefaultApiServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DefaultApiServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
