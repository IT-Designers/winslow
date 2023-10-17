import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ProjectHistoryHeaderComponent } from './project-history-header.component';

describe('ProjectHistoryHeaderComponent', () => {
  let component: ProjectHistoryHeaderComponent;
  let fixture: ComponentFixture<ProjectHistoryHeaderComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ProjectHistoryHeaderComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectHistoryHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
