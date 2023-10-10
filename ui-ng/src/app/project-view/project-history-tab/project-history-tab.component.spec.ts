import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectHistoryTabComponent } from './project-history-tab.component';

describe('ProjectHistoryTabComponent', () => {
  let component: ProjectHistoryTabComponent;
  let fixture: ComponentFixture<ProjectHistoryTabComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ProjectHistoryTabComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectHistoryTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
