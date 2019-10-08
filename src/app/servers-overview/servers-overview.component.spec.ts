import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServersOverviewComponent } from './servers-overview.component';

describe('ServersOverviewComponent', () => {
  let component: ServersOverviewComponent;
  let fixture: ComponentFixture<ServersOverviewComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServersOverviewComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServersOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
