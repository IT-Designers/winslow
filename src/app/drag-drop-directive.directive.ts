import {Directive, EventEmitter, HostBinding, HostListener, Output} from '@angular/core';

@Directive({
  selector: '[appDragDrop]'
})
export class DragDropDirectiveDirective {

  @Output() onFileDropped = new EventEmitter<any>();

  @HostBinding('class.dragover') private dragover = false;

  @HostListener('dragover', ['$event'])
  onDragOver(event) {
    event.preventDefault();
    event.stopPropagation();
    this.dragover = true;
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(event) {
    event.preventDefault();
    event.stopPropagation();
    this.dragover = false;
  }

  @HostListener('drop', ['$event'])
  onDrop(event) {
    event.preventDefault();
    event.stopPropagation();
    this.dragover = false;
    const files = event.dataTransfer.files;
    if (files.length > 0) {
      this.onFileDropped.emit(files);
    }
  }

}
