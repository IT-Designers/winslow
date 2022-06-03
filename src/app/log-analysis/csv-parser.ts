export type CsvFileContent = string[][]

export type CsvParserOptions = {
  rowDelimiters: string[]
  colDelimiters: string[]
  quotationMarks: string[]
}

// tokens are searched for in given order
const DEFAULT_OPTIONS: CsvParserOptions = {
  rowDelimiters: ['\r\n', '\n\r', '\r', '\n'],
  colDelimiters: [',', ';', '\t'],
  quotationMarks: ['"'],
}

export function parseCsv(text: string, formatOptions = DEFAULT_OPTIONS): CsvFileContent {
  const textLength = text.length

  let parsed: CsvFileContent = []

  let nextRow: string[] = []
  let expectFieldContent = false
  let index = 0;

  while (index < textLength) {
    const nextToken = findToken(text, index, formatOptions)
    switch (nextToken.type) {

      // end of file
      case TokenType.NONE_FOUND:
        if (expectFieldContent) nextRow.push(text.substring(index, textLength))
        expectFieldContent = true
        parsed.push(nextRow)
        nextRow = []
        index = textLength
        break;

      // end of line
      case TokenType.ROW_DELIMITER:
        if (expectFieldContent) nextRow.push(text.substring(index, nextToken.index))
        expectFieldContent = true
        parsed.push(nextRow)
        nextRow = []
        index = nextToken.index + nextToken.literal.length
        break;

      // end of field
      case TokenType.COL_DELIMITER:
        if (expectFieldContent) nextRow.push(text.substring(index, nextToken.index))
        expectFieldContent = true
        index = nextToken.index + nextToken.literal.length
        break;

      // start of quote
      case TokenType.QUOTATION_MARK:
        const quoteStartIndex = nextToken.index + nextToken.literal.length
        const quoteEndIndex = text.indexOf(nextToken.literal, quoteStartIndex + nextToken.literal.length)
        nextRow.push(text.substring(quoteStartIndex, quoteEndIndex))
        expectFieldContent = false
        index = quoteEndIndex + nextToken.literal.length
        break;
    }
  }

  console.log(text)
  console.log(parsed)

  return parsed
}

enum TokenType {
  NONE_FOUND,
  ROW_DELIMITER,
  COL_DELIMITER,
  QUOTATION_MARK,
}

interface SearchResult {
  index: number
  literal: string
  type: TokenType
}

function findToken(source: string, start: number, options: CsvParserOptions): SearchResult {
  try {
    return ([
      ...options.rowDelimiters.map(token => ({
        index: source.indexOf(token, start),
        literal: token,
        type: TokenType.ROW_DELIMITER
      })),
      ...options.colDelimiters.map(token => ({
        index: source.indexOf(token, start),
        literal: token,
        type: TokenType.COL_DELIMITER
      })),
      ...options.quotationMarks.map(token => ({
        index: source.indexOf(token, start),
        literal: token,
        type: TokenType.QUOTATION_MARK
      })),
    ]).filter(result => result.index != -1)
      .reduce((prev, current) => prev.index < current.index ? prev : current)
  } catch (error) {
    return {
      index: -1,
      literal: "",
      type: TokenType.NONE_FOUND,
    }
  }
}
