import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogAnalysisComponent } from './log-analysis.component';

describe('LogAnalysisComponent', () => {
  let component: LogAnalysisComponent;
  let fixture: ComponentFixture<LogAnalysisComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LogAnalysisComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LogAnalysisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
