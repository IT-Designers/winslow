import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectViewHeaderComponent } from './project-view-header.component';

describe('ProjectHeaderComponent', () => {
  let component: ProjectViewHeaderComponent;
  let fixture: ComponentFixture<ProjectViewHeaderComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ProjectViewHeaderComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectViewHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
