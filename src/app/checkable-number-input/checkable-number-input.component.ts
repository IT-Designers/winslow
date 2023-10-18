import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

@Component({
  selector: 'app-checkable-number-input',
  templateUrl: './checkable-number-input.component.html',
  styleUrls: ['./checkable-number-input.component.css']
})
export class CheckableNumberInputComponent implements OnInit {

  @Input() checkable = true;
  @Input() revertible = true;
  @Input() minValue = 1;
  @Input() name?: string;
  @Input() unit?: string;

  @Output() valueChange: EventEmitter<number> = new EventEmitter<number>();

  value?: number;
  valueOriginal?: number;

  constructor() { }

  ngOnInit(): void {
  }


  onCheckedUpdate($event: Event) {
    if (($event.target as HTMLInputElement).checked) {
      this.value = this.valueOriginal ?? this.minValue;
    } else {
      this.value = undefined;
    }
    this.onValueUpdate();
  }

  onValueUpdate($event?: Event) {
    if ($event != null) {
      this.valueChange.emit(Number(($event.target as HTMLInputElement).value));
    } else {
      this.valueChange.emit(this.value);
    }
  }

  @Input('value')
  public set valueSetter(value: number) {
    this.value = value;
    this.valueOriginal = value;
  }
}
