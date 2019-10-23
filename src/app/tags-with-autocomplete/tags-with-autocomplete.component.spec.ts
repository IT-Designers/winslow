import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TagsWithAutocompleteComponent } from './tags-with-autocomplete.component';

describe('TagsWithAutocompleteComponent', () => {
  let component: TagsWithAutocompleteComponent;
  let fixture: ComponentFixture<TagsWithAutocompleteComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TagsWithAutocompleteComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TagsWithAutocompleteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
