import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupsDropdownComponent } from './groups-dropdown.component';

describe('GroupsDropdownComponent', () => {
  let component: GroupsDropdownComponent;
  let fixture: ComponentFixture<GroupsDropdownComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GroupsDropdownComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupsDropdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
