import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectOverviewTabComponent } from './project-overview-tab.component';

describe('ProjectInfoComponent', () => {
  let component: ProjectOverviewTabComponent;
  let fixture: ComponentFixture<ProjectOverviewTabComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ProjectOverviewTabComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectOverviewTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
