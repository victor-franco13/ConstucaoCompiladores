Como compilar o projeto:

javac -encoding UTF-8 -d out src\lexer\*.java src\Main.java test\lexer\*.java

java -Dfile.encoding=UTF-8 -cp out lexer.ScannerTest

java -Dfile.encoding=UTF-8 -cp out Main exemplos\programa.mini
java -Dfile.encoding=UTF-8 -cp out Main exemplos\erros.mini