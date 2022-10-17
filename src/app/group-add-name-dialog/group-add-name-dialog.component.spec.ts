import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupAddNameDialogComponent } from './group-add-name-dialog.component';

describe('GroupAddNameDialogComponent', () => {
  let component: GroupAddNameDialogComponent;
  let fixture: ComponentFixture<GroupAddNameDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GroupAddNameDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupAddNameDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
