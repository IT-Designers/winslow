import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CreatePipelineDialogComponent } from './create-pipeline-dialog.component';

describe('DialogCreatePipelineComponent', () => {
  let component: CreatePipelineDialogComponent;
  let fixture: ComponentFixture<CreatePipelineDialogComponent>;

  beforeEach(async(() => {
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
