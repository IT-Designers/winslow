import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ProjectHistoryComponent } from './project-history.component';

describe('ProjectHistoryComponent', () => {
  let component: ProjectHistoryComponent;
  let fixture: ComponentFixture<ProjectHistoryComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ProjectHistoryComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
