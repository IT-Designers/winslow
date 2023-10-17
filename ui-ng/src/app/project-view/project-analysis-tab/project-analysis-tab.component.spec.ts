import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectAnalysisTabComponent } from './project-analysis-tab.component';

describe('ProjectAnalysisTabComponent', () => {
  let component: ProjectAnalysisTabComponent;
  let fixture: ComponentFixture<ProjectAnalysisTabComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectAnalysisTabComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectAnalysisTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
