import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditFormsComponent } from './edit-forms.component';

describe('EditFormsComponent', () => {
  let component: EditFormsComponent;
  let fixture: ComponentFixture<EditFormsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EditFormsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EditFormsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
