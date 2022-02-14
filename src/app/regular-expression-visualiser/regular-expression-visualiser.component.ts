import {Component, Input, OnInit} from '@angular/core';

enum SegmentType {
  NORMAL = "segment-normal",
  MATCH = "segment-match",
  GROUP = "segment-group",
  UNCERTAIN = "segment-uncertain",
}

class Segment {
  text: string;
  classType: SegmentType;

  constructor(source: string, start: number, end: number, type: SegmentType) {
    this.classType = type;
    this.text = source.slice(start, end);
  }
}

@Component({
  selector: 'app-regular-expression-visualiser',
  templateUrl: './regular-expression-visualiser.component.html',
  styleUrls: ['./regular-expression-visualiser.component.css']
})
export class RegularExpressionVisualiserComponent implements OnInit {

  @Input() text: string;

  // Ensures 'd'-flag is set
  @Input() set pattern(pattern: string) {
    this.regExp = new RegExp(pattern, "d");
  }

  regExp: RegExp;

  constructor() {
  }

  ngOnInit(): void {
  }

  getSegments(text: string): Segment[] {
    const match = text.match(this.regExp);

    if (!match) {
      return [{text: text, classType: SegmentType.NORMAL}];
    }
    try {
      // Requires this.regExp to have 'd'-flag set.
      const indices = match["indices"];

      // The first element in indices contains the start and end of the match.
      // The following elements contain the start and end of each group within that match.
      const textStart = 0;
      const textEnd = text.length - 1;
      const matchStart = indices[0][0];
      const matchEnd = indices[0][1];

      const segments: Segment[] = [];

      // add non-match part before match
      segments.push(new Segment(text, textStart, matchStart, SegmentType.NORMAL));
      if (indices.length == 1) { // no capturing groups
        // add the entire match
        segments.push(new Segment(text, matchStart, matchEnd, SegmentType.MATCH));
      } else { // some capturing groups
        // add non-group part before first group
        segments.push(new Segment(text, matchStart, indices[1][0], SegmentType.MATCH));
        let group;
        for (group = 1; group < indices.length - 1; group++) {
          // add group
          segments.push(new Segment(text, indices[group][0], indices[group][1], SegmentType.GROUP));
          // add non-group part between two groups
          segments.push(new Segment(text, indices[group][1], indices[group + 1][0], SegmentType.MATCH));
        }
        // add last group
        segments.push(new Segment(text, indices[group][0], indices[group][1], SegmentType.GROUP));
        // add non-group part after last group
        segments.push(new Segment(text, indices[group][1], matchEnd, SegmentType.MATCH));
      }
      // add non-match part after match
      segments.push(new Segment(text, matchEnd, textEnd, SegmentType.NORMAL));
      return segments;

    } catch (error) {
      console.warn(`Missing indices for match ${match.input}. Is the 'd'-flag set?`);
      console.warn(error);
      return [{text: text, classType: SegmentType.UNCERTAIN}];
    }
  }
}
