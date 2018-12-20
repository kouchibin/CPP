# CPP
Type checker and interpreter as well as compiler for "CPP" language implemented in Java.

Note: This is a course project from [Programming Language Technology](http://www.cse.chalmers.se/edu/course/DAT151_Programming_Language_Technology/) at Chalmers.


- CPP.cf - Concrete syntax for CPP language.
- TypeChecker.java - Type checker class.
- Interpreter.java - Interpreter class.
- Compiler.java - Compiler class.

The abstract syntax tree is generated from CPP.cf using [BNFC](https://bnfc.digitalgrammars.com/) tool. [Visitor Design Pattern](https://en.wikipedia.org/wiki/Visitor_pattern) is used for implementing type checker and interpreter.
