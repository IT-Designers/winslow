import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupsAddMemberDialogComponent } from './groups-add-member-dialog.component';

describe('GroupsAddMemberDialogComponent', () => {
  let component: GroupsAddMemberDialogComponent;
  let fixture: ComponentFixture<GroupsAddMemberDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GroupsAddMemberDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupsAddMemberDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
