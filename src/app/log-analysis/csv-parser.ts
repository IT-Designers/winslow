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
  let startOfNewField = false
  let index = 0;

  while (index < textLength) {
    const nextToken = findToken(text, index, formatOptions)
    switch (nextToken.type) {

      // end of file
      case TokenType.NONE_FOUND:
        if (startOfNewField) nextRow.push(text.substring(index, textLength))
        startOfNewField = true
        parsed.push(nextRow)
        nextRow = []
        index = textLength
        break;

      // end of line
      case TokenType.ROW_DELIMITER:
        if (startOfNewField) nextRow.push(text.substring(index, nextToken.index))
        startOfNewField = true
        parsed.push(nextRow)
        nextRow = []
        index = nextToken.index + nextToken.literal.length
        break;

      // end of field
      case TokenType.COL_DELIMITER:
        if (startOfNewField) nextRow.push(text.substring(index, nextToken.index))
        startOfNewField = true
        index = nextToken.index + nextToken.literal.length
        break;

      // start of quote
      case TokenType.QUOTATION_MARK:
        const quoteStartIndex = nextToken.index + nextToken.literal.length
        const quotedFieldInfo = parseQuotedField(text, quoteStartIndex, nextToken.literal)
        nextRow.push(quotedFieldInfo.content)
        startOfNewField = false
        index = quotedFieldInfo.endIndex
        break;
    }
  }

  console.log(text)
  console.log(parsed)

  return parsed
}

function parseQuotedField(text: string, startIndex: number, quotationMark: string) {
  const escapeSequence = `${quotationMark}${quotationMark}`
  let quoteContent = ""
  let index = startIndex

  while (true) {
    const nextQuotationMark = text.indexOf(quotationMark, startIndex)
    const nextEscapeSequence = text.indexOf(escapeSequence, startIndex)
    if (nextQuotationMark == -1) {
      console.warn("Unresolved quote in csv file.")
      quoteContent += text.substring(index)
      index = text.length
      break;
    } else if (nextEscapeSequence == -1 || nextQuotationMark < nextEscapeSequence) {
      quoteContent += text.substring(index, nextQuotationMark)
      index = nextQuotationMark + quotationMark.length
      break;
    } else {
      quoteContent += text.substring(index, nextEscapeSequence) + quotationMark
      index = nextEscapeSequence + escapeSequence.length
    }
  }

  return {
    endIndex: index,
    content: quoteContent
  }
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

function findToken(text: string, start: number, options: CsvParserOptions): SearchResult {
  try {
    return ([
      ...options.rowDelimiters.map(token => ({
        index: text.indexOf(token, start),
        literal: token,
        type: TokenType.ROW_DELIMITER
      })),
      ...options.colDelimiters.map(token => ({
        index: text.indexOf(token, start),
        literal: token,
        type: TokenType.COL_DELIMITER
      })),
      ...options.quotationMarks.map(token => ({
        index: text.indexOf(token, start),
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
