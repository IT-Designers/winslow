import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServerNewComponent } from './server-new.component';

describe('ServerNewComponent', () => {
  let component: ServerNewComponent;
  let fixture: ComponentFixture<ServerNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ServerNewComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServerNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
