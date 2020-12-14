import { TestBed } from '@angular/core/testing';

import { SettingsApiService } from './settings-api.service';

describe('EnvSettingsApiService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: SettingsApiService = TestBed.get(SettingsApiService);
    expect(service).toBeTruthy();
  });
});
