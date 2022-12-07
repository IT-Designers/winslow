import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServerGroupsListComponent } from './server-groups-list.component';

describe('ServerGroupsListComponent', () => {
  let component: ServerGroupsListComponent;
  let fixture: ComponentFixture<ServerGroupsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ServerGroupsListComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServerGroupsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
