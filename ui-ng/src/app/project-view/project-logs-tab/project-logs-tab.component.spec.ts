import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectLogsTabComponent } from './project-logs-tab.component';

describe('ProjectLogsTabComponent', () => {
  let component: ProjectLogsTabComponent;
  let fixture: ComponentFixture<ProjectLogsTabComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectLogsTabComponent ]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectLogsTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
