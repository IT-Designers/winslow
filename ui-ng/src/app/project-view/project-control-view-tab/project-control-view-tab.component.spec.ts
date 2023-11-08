import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectControlViewTabComponent } from './project-control-view-tab.component';

describe('ProjectControlViewTabComponent', () => {
  let component: ProjectControlViewTabComponent;
  let fixture: ComponentFixture<ProjectControlViewTabComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ProjectControlViewTabComponent]
    });
    fixture = TestBed.createComponent(ProjectControlViewTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
