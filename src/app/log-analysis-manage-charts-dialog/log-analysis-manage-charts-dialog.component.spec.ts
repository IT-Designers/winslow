import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogAnalysisManageChartsDialogComponent } from './log-analysis-manage-charts-dialog.component';

describe('LogAnalysisManageChartsDialogComponent', () => {
  let component: LogAnalysisManageChartsDialogComponent;
  let fixture: ComponentFixture<LogAnalysisManageChartsDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LogAnalysisManageChartsDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LogAnalysisManageChartsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
