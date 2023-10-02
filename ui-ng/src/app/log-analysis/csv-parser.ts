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
};

enum TokenType {
  NoToken,
  ColDelimiter,
  RowDelimiter,
  QuotationMark,
}

type TokenInfo = {
  literal: string,
  type: TokenType
}

enum State {
  BeforeQuote,
  InsideQuote,
  BeyondQuote,
}

function toTokenList(options: CsvParserOptions): TokenInfo[] {
  return [
    ...options.rowDelimiters.map(token => ({
      literal: token,
      type: TokenType.RowDelimiter
    })),
    ...options.colDelimiters.map(token => ({
      literal: token,
      type: TokenType.ColDelimiter
    })),
    ...options.quotationMarks.map(token => ({
      literal: token,
      type: TokenType.QuotationMark
    })),
  ];
}

export function parseCsv(text: string, options: CsvParserOptions = DEFAULT_OPTIONS) {
  const tokens: TokenInfo[] = toTokenList(options);

  let fileContent = [];
  let lineContent = [];
  let fieldContent = '';

  function resetField() {
    fieldContent = '';
  }

  function pushCharsToField(literal: string) {
    fieldContent = fieldContent.concat(literal);
  }

  function pushFieldToLine() {
    lineContent.push(fieldContent);
    fieldContent = '';
  }

  function pushLineToFile() {
    fileContent.push(lineContent);
    lineContent = [];
  }

  let parserState: State = State.BeforeQuote;
  let position: number = 0;

  while (position < text.length) {

    // check if there is a token at the current position
    let increment: number = 1;
    let tokenType: TokenType = TokenType.NoToken;
    let tokenLiteral: string = null;

    for (const token of tokens) {
      const tokenLength = token.literal.length;
      if (text.slice(position, position + tokenLength) == token.literal) {
        tokenType = token.type;
        tokenLiteral = token.literal;
        increment = tokenLength;
        break;
      }
    }

    // handle or ignore the token depending on quotes
    switch (parserState) {
      case State.BeforeQuote:
        switch (tokenType) {
          case TokenType.NoToken:
            pushCharsToField(text.charAt(position));
            break;
          case TokenType.ColDelimiter:
            pushFieldToLine();
            break;
          case TokenType.RowDelimiter:
            pushFieldToLine();
            pushLineToFile();
            break;
          case TokenType.QuotationMark:
            resetField();
            parserState = State.InsideQuote;
            break;
        }
        break;

      case State.InsideQuote:
        switch (tokenType) {
          case TokenType.NoToken:
          case TokenType.ColDelimiter:
          case TokenType.RowDelimiter:
            pushCharsToField(text.charAt(position));
            break;
          case TokenType.QuotationMark:
            parserState = State.BeyondQuote;
            break;
        }
        break;

      case State.BeyondQuote:
        switch (tokenType) {
          case TokenType.NoToken:
            break;
          case TokenType.ColDelimiter:
            pushFieldToLine();
            parserState = State.BeforeQuote;
            break;
          case TokenType.RowDelimiter:
            pushLineToFile();
            parserState = State.BeforeQuote;
            break;
          case TokenType.QuotationMark:
            pushCharsToField(tokenLiteral);
            parserState = State.InsideQuote;
            break;
        }
        break;
    }
    position += increment;
  }

  if (lineContent.length > 0) {
    pushFieldToLine();
    pushLineToFile();
  }

  console.log(fileContent)
  return fileContent;
}

