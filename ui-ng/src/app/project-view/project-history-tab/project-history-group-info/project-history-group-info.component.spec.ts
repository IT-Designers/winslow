import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ProjectHistoryGroupInfoComponent } from './project-history-group-info.component';

describe('ProjectHistoryGroupInfoComponent', () => {
  let component: ProjectHistoryGroupInfoComponent;
  let fixture: ComponentFixture<ProjectHistoryGroupInfoComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ProjectHistoryGroupInfoComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectHistoryGroupInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
