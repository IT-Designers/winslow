import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProjectControlTabComponent } from './project-control-tab.component';

describe('ProjectControlTabComponent', () => {
  let component: ProjectControlTabComponent;
  let fixture: ComponentFixture<ProjectControlTabComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectControlTabComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectControlTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
