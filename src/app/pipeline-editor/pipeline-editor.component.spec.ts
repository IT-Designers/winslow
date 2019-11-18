import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PipelineEditorComponent } from './pipeline-editor.component';

describe('PipelineEditorComponent', () => {
  let component: PipelineEditorComponent;
  let fixture: ComponentFixture<PipelineEditorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PipelineEditorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PipelineEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
