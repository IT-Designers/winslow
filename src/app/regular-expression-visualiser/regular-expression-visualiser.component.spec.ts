import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegularExpressionVisualiserComponent } from './regular-expression-visualiser.component';

describe('RegularExpressionVisualiserComponent', () => {
  let component: RegularExpressionVisualiserComponent;
  let fixture: ComponentFixture<RegularExpressionVisualiserComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RegularExpressionVisualiserComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RegularExpressionVisualiserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
