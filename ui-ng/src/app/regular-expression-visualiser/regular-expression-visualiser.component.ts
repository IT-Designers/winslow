import {Component, Input, OnInit} from '@angular/core';

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

  regExp!: RegExp;

  constructor() {
    this.text = "";
    this.pattern = "";
  }

  ngOnInit(): void {
  }

  getSegments(text: string): Segment[] {
    const regExpMatchArray = text.match(this.regExp);

    if (regExpMatchArray == null) {
      return [
        new Segment(SegmentType.NORMAL, text)
      ];
    }

    try {
      const textStart = 0;
      const textEnd = text.length;
      // Requires this.regExp to have 'd'-flag set.
      const match = RegExpMatchIndices.fromRegExpMatchArray(regExpMatchArray);

      let segments = [];
      segments.push(new Segment(SegmentType.NORMAL, text.substring(textStart, match.start)));
      segments.push(...RegularExpressionVisualiserComponent.segmentsOfMatch(match, text));
      segments.push(new Segment(SegmentType.NORMAL, text.substring(match.end, textEnd)))
      return segments;

    } catch (error) {
      return [
        new Segment(SegmentType.UNCERTAIN, text)
      ];
    }
  }

  private static segmentsOfMatch(match: RegExpMatchIndices, text: string): Segment[] {
    const matchHasGroups = match.groups.length > 0;

    if (!matchHasGroups) {
      return [new Segment(SegmentType.MATCH, text.substring(match.start, match.end))];
    }

    let segments = [];
    const lastIndex = match.groups.length - 1;

    segments.push(new Segment(SegmentType.MATCH, text.substring(match.start, (match.groups)[0].start)));
    for (let i = 0; i < lastIndex; i++) {
      segments.push(new Segment(SegmentType.GROUP, text.substring(match.groups[i].start, match.groups[i].end)));
      segments.push(new Segment(SegmentType.MATCH, text.substring(match.groups[i].end, match.groups[i + 1].start)));
    }
    segments.push(new Segment(SegmentType.GROUP, text.substring(match.groups[lastIndex].start, match.groups[lastIndex].end)));
    segments.push(new Segment(SegmentType.MATCH, text.substring(match.groups[lastIndex].end, match.end)));

    return segments;
  }
}

enum SegmentType {
  NORMAL = "segment-normal",
  MATCH = "segment-match",
  GROUP = "segment-group",
  UNCERTAIN = "segment-uncertain",
}

class Segment {
  text: string;
  classType: SegmentType;

  constructor(type: SegmentType, text: string) {
    this.classType = type;
    this.text = text;
  }
}

type RegexArrayWithIndices = RegExpMatchArray & { indices?: [number, number][] }

class RegExpMatchIndices {
  readonly start: number;
  readonly end: number;
  readonly groups: { start: number, end: number }[];

  private constructor(indices: [number, number][]) {
    this.start = indices[0][0]
    this.end = indices[0][1]
    this.groups = indices.slice(1).map(groupIndices => ({
      start: groupIndices[0],
      end: groupIndices[1]
    }))
  }

  static fromRegExpMatchArray(match: RegexArrayWithIndices): RegExpMatchIndices {
    if (match.indices != undefined) {
      return new RegExpMatchIndices(match.indices);
    } else {
      const message = `Missing indices for match ${match.input}. Is the 'd'-flag set?`;
      console.error(message);
      throw new Error(message);
    }
  }
}
