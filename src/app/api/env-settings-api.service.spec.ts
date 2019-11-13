import { TestBed } from '@angular/core/testing';

import { EnvSettingsApiService } from './env-settings-api.service';

describe('EnvSettingsApiService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: EnvSettingsApiService = TestBed.get(EnvSettingsApiService);
    expect(service).toBeTruthy();
  });
});
