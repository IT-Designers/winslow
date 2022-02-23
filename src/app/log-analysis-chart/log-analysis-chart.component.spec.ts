import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogAnalysisChartComponent } from './log-analysis-chart.component';

describe('LogAnalysisChartComponent', () => {
  let component: LogAnalysisChartComponent;
  let fixture: ComponentFixture<LogAnalysisChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LogAnalysisChartComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LogAnalysisChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
