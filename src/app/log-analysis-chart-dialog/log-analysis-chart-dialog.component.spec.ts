import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogAnalysisChartDialogComponent } from './log-analysis-chart-dialog.component';

describe('LogAnalysisChartDialogComponent', () => {
  let component: LogAnalysisChartDialogComponent;
  let fixture: ComponentFixture<LogAnalysisChartDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LogAnalysisChartDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LogAnalysisChartDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
