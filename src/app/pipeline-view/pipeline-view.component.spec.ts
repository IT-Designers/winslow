import { TestBed, ComponentFixture, waitForAsync } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { PipelineViewComponent } from './pipeline-view.component';
import { DiagramComponent } from './diagram/diagram.component';
import { DebugNode } from '@angular/core';

describe('PipelineViewComponent', () => {
  let fixture: ComponentFixture<PipelineViewComponent>;
  let component: DebugNode['componentInstance'];

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [
        PipelineViewComponent,
        DiagramComponent
      ],
      imports: [HttpClientTestingModule]
    }).compileComponents();
    fixture = TestBed.createComponent(PipelineViewComponent);
    component = fixture.debugElement.componentInstance;
    fixture.detectChanges();
  }));


  it('should create', () => {
    expect(component).toBeTruthy();
  });


  it('renders a diagram component', () => {
    expect(fixture.nativeElement.querySelector('diagram')).toBeTruthy();
  });


  it('sets an error message', () => {
    const error = new Error('ERROR');

    component.handleImported({
      type: 'error',
      error
    });

    expect(component.importError).toEqual(error);
  });
});
