export type CsvFileContent = string[][]

export type CsvParserOptions = {
  rowDelimiters: string[]
  colDelimiters: string[]
  quotationMarks: string[]
}

const DEFAULT_OPTIONS: CsvParserOptions = {
  rowDelimiters: ['\r\n', '\r', '\n'],
  colDelimiters: [',', ';', '\t'],
  quotationMarks: ['"'],
}

export function parseCsv(text: string, formatOptions = DEFAULT_OPTIONS): CsvFileContent {
  const textLength = text.length
  const {rowDelimiters, colDelimiters, quotationMarks} = formatOptions
  const allTokens = [...rowDelimiters, ...colDelimiters, ...quotationMarks]

  let parsed: CsvFileContent = []

  let nextRow: string[] = []
  let expectFieldContent = false
  let index = 0;

  while (index < textLength) {
    const nextToken = findToken(text, index, allTokens)

    // end of file
    if (nextToken.index == -1) {
      if (expectFieldContent) nextRow.push(text.substring(index, textLength))
      expectFieldContent = true
      parsed.push(nextRow)
      nextRow = []
      index = textLength
    }

    // end of line
    else if (rowDelimiters.includes(nextToken.literal)) {
      if (expectFieldContent) nextRow.push(text.substring(index, nextToken.index))
      expectFieldContent = true
      parsed.push(nextRow)
      nextRow = []
      index = nextToken.index + nextToken.literal.length
    }

    // end of field
    else if (colDelimiters.includes(nextToken.literal)) {
      if (expectFieldContent) nextRow.push(text.substring(index, nextToken.index))
      expectFieldContent = true
      index = nextToken.index + nextToken.literal.length
    }

    // start of quote
    else if (quotationMarks.includes(nextToken.literal)) {
      const quoteStartIndex = nextToken.index + nextToken.literal.length
      const quoteEndIndex = text.indexOf(nextToken.literal, quoteStartIndex + nextToken.literal.length)
      nextRow.push(text.substring(quoteStartIndex, quoteEndIndex))
      expectFieldContent = false
      index = quoteEndIndex + nextToken.literal.length
      //todo escape
    }
  }

  console.log(text)
  console.log(parsed)

  return parsed
}

interface SearchResult {
  index: number
  literal: string
}

function findToken(source: string, start: number, tokens: string[]): SearchResult {
  return tokens
    .map(token => ({index: source.indexOf(token, start), literal: token}))
    .filter(result => result.index != -1)
    .reduce((prev, current) => prev.index < current.index ? prev : current)
}
