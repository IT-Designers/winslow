import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupAddMemberDialogComponent } from './group-add-member-dialog.component';

describe('GroupsAddMemberDialogComponent', () => {
  let component: GroupAddMemberDialogComponent;
  let fixture: ComponentFixture<GroupAddMemberDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GroupAddMemberDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupAddMemberDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
