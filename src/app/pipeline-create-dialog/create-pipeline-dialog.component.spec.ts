import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { CreatePipelineDialogComponent } from './create-pipeline-dialog.component';

describe('DialogCreatePipelineComponent', () => {
  let component: CreatePipelineDialogComponent;
  let fixture: ComponentFixture<CreatePipelineDialogComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ CreatePipelineDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CreatePipelineDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
