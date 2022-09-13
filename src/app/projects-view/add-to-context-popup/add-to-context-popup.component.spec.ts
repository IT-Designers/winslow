import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddToContextPopupComponent } from './add-to-context-popup.component';

describe('AddToContextPopupComponent', () => {
  let component: AddToContextPopupComponent;
  let fixture: ComponentFixture<AddToContextPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AddToContextPopupComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AddToContextPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
