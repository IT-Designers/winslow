import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectsGroupBuilderComponent } from './projects-group-builder.component';

describe('ProjectsGroupBuilderComponent', () => {
  let component: ProjectsGroupBuilderComponent;
  let fixture: ComponentFixture<ProjectsGroupBuilderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectsGroupBuilderComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectsGroupBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
