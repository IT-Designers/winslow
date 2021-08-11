import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectHistoryItemComponent } from './project-history-item.component';

describe('ProjectHistoryItemComponent', () => {
  let component: ProjectHistoryItemComponent;
  let fixture: ComponentFixture<ProjectHistoryItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectHistoryItemComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectHistoryItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
