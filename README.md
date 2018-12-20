# CPP
Type checker, interpreter, compiler for "CPP" language implemented in Java.

Note: This is a course project from [Programming Language Technology](http://www.cse.chalmers.se/edu/course/DAT151_Programming_Language_Technology/) at Chalmers. This is a really great course by the way.


- CPP.cf - Concrete syntax for CPP language.
- TypeChecker.java - Type checker class.
- Interpreter.java - Interpreter class.
- Compiler.java - Compiler class.
- functional-interpreter/ - Interpreter for a functional programming language similar to Haskell.

The abstract syntax tree is generated from CPP.cf using [BNFC](https://bnfc.digitalgrammars.com/) tool. [Visitor Design Pattern](https://en.wikipedia.org/wiki/Visitor_pattern) is used for implementing type checker and interpreter.
