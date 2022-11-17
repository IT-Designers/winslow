import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DiagramGatewayComponent } from './diagram-gateway.component';

describe('DiagramGatewayComponent', () => {
  let component: DiagramGatewayComponent;
  let fixture: ComponentFixture<DiagramGatewayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DiagramGatewayComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DiagramGatewayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
