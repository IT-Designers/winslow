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

  @Input() textSource: string[];

  // Ensures 'd'-flag is set
  @Input() set pattern(pattern: string) {
    this.regExp = new RegExp(pattern, "d");
  }

  regExp: RegExp;

  constructor() {
  }

  ngOnInit(): void {
  }

  getSegments(line: string): Segment[] {
    let match = line.match(this.regExp);

    if (!match) {
      return [{text: line, classType: SegmentType.NORMAL}];
    }
    try {
      // Requires this.regExp to have 'd'-flag set.
      // @ts-ignore
      let {indices}: { indices: [number, number][] } = match;

      // The first element in indices contains the start and end of the match.
      // The following elements contain the start and end of each group within that match.
      let lineStart = 0;
      let lineEnd = line.length - 1;
      let matchStart = indices[0][0];
      let matchEnd = indices[0][1];

      let segments: Segment[] = [];
      segments.push(new Segment(line, lineStart, matchStart, SegmentType.NORMAL));

      if (indices.length == 1) {
        segments.push(new Segment(line, matchStart, matchEnd, SegmentType.MATCH));
      } else {
        segments.push(new Segment(line, matchStart, indices[1][0], SegmentType.MATCH));
        let groupIndex;
        for (groupIndex = 1; groupIndex < indices.length - 1; groupIndex++) {
          segments.push(new Segment(line, indices[groupIndex][0], indices[groupIndex][1], SegmentType.GROUP));
          segments.push(new Segment(line, indices[groupIndex][1], indices[groupIndex + 1][0], SegmentType.MATCH));
        }
        segments.push(new Segment(line, indices[groupIndex][0], indices[groupIndex][1], SegmentType.GROUP));
        segments.push(new Segment(line, indices[groupIndex][1], matchEnd, SegmentType.MATCH));
      }

      segments.push(new Segment(line, matchEnd, lineEnd, SegmentType.NORMAL));
      return segments;

    } catch (error) {
      // If a match exists but the indices are missing, the entire line gets marked as a potential match.
      return [{text: line, classType: SegmentType.UNCERTAIN}];
    }
  }
}
