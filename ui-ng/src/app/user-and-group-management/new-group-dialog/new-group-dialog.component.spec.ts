import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NewGroupDialogComponent } from './new-group-dialog.component';

describe('GroupAddNameDialogComponent', () => {
  let component: NewGroupDialogComponent;
  let fixture: ComponentFixture<NewGroupDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ NewGroupDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NewGroupDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
