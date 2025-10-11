# SPL
Student Programming Language for COS341

# Structure
```SPL-Compiler/
├── src/
│   ├── main
│   │   └── java
│   │       ├── Main.java
│   │       ├── lexer/
│   │       │   ├── Token.java
│   │       │   ├── TokenType.java
│   │       │   ├── Lexer.java
│   │       │   └── LexerException.java
│   │       ├── parser/
│   │       │   ├── Parser.java
│   │       │   ├── ParserException.java
│   │       │   └── ast/
│   │       ├── semantic/
│   │       │   ├── SemanticAnalyzer.java
│   │       │   ├── SymbolTable.java
│   │       │   ├── Symbol.java
│   │       │   ├── Scope.java
│   │       │   ├── SemanticException.java
│   │       │   └── TypeChecker.java
│   │       ├── codegen/
│   │       │   ├── CodeGenerator.java
│   │       │   ├── TargetCode.java
│   │       │   └── CodeGenException.java
│   │       └── util/
│   │           ├── ErrorHandler.java
│   │           ├── SourceFile.java
│   │           └── CompilerConfig.java
│   └── test/
│        └── java
│            ├── lexer
│            │   └── LexerTest.java
│            ├── lexer/
│            │   └── LexerTest.java
│            ├── parser/
│            │   └── ParserTest.java
│            ├── semantic/
│            │   └── SemanticAnalyzerTest.java
│            └── integration/
│                └── EndToEndTest.java
├── resources/
│   ├── test_programs/
│   │   ├── simple.spl
│   │   ├── factorial.spl
│   │   └── ... (more test SPL programs)
│   └── grammar.txt
├── docs/
├── build/
├── lib/
├── pom.xml
├── README.md
└── .gitignore
```