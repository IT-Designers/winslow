import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogAnalysisSettingsDialogComponent } from './log-analysis-settings-dialog.component';

describe('LogAnalysisSettingsDialogComponent', () => {
  let component: LogAnalysisSettingsDialogComponent;
  let fixture: ComponentFixture<LogAnalysisSettingsDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LogAnalysisSettingsDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LogAnalysisSettingsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
