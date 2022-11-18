import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserAddNameDialogComponent } from './user-add-name-dialog.component';

describe('UserAddNameDialogComponent', () => {
  let component: UserAddNameDialogComponent;
  let fixture: ComponentFixture<UserAddNameDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UserAddNameDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(UserAddNameDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
