import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectHistoryNewComponent } from './project-history-new.component';

describe('ProjectHistoryNewComponent', () => {
  let component: ProjectHistoryNewComponent;
  let fixture: ComponentFixture<ProjectHistoryNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectHistoryNewComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectHistoryNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
