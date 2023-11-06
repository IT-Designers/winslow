import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectThumbnailComponent } from './project-thumbnail.component';

describe('ProjectAvatarComponent', () => {
  let component: ProjectThumbnailComponent;
  let fixture: ComponentFixture<ProjectThumbnailComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ProjectThumbnailComponent]
    });
    fixture = TestBed.createComponent(ProjectThumbnailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
