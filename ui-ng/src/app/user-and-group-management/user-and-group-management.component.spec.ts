import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserAndGroupManagementComponent } from './user-and-group-management.component';

describe('GroupsViewComponent', () => {
  let component: UserAndGroupManagementComponent;
  let fixture: ComponentFixture<UserAndGroupManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UserAndGroupManagementComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(UserAndGroupManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
