import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegularExpressionEditorDialogComponent } from './regular-expression-editor-dialog.component';

describe('RegularExpressionEditorDialogComponent', () => {
  let component: RegularExpressionEditorDialogComponent;
  let fixture: ComponentFixture<RegularExpressionEditorDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RegularExpressionEditorDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RegularExpressionEditorDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
