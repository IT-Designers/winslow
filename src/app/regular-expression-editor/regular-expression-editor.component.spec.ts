import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegularExpressionEditorComponent } from './regular-expression-editor.component';

describe('RegularExpressionEditorComponent', () => {
  let component: RegularExpressionEditorComponent;
  let fixture: ComponentFixture<RegularExpressionEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RegularExpressionEditorComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RegularExpressionEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
