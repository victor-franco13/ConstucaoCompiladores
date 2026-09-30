# Especificação Léxica — Linguagem **Mini**

Construção de Compiladores · UFU/FACOM/BCC · Checkpoint 1, Parte 1


## 1. Definições auxiliares (EBNF / regex)

```ebnf
letra      = "a".."z" | "A".."Z" ;                    (* só ASCII, sem acentos *)
digito     = "0".."9" ;
espaco     = " " | "\t" | "\r" | "\n" ;
nova_linha = "\n" | "\r" ;
escape     = "\" ( "n" | "t" | '"' | "'" | "\" ) ;
```

## 2. As cinco categorias de token

| Token | Notação (regex/EBNF) | Exemplos válidos | Decisões da linguagem |
|---|---|---|---|
| **Identificador** `[IDENTIFIER]` | `letra ( letra \| digito \| "_" )*` | `total`, `x1`, `contaItens`, `conta_itens` | Case-sensitive. `_` permitido, **exceto como 1º caractere**. Tamanho máximo **31** caracteres; acima disso o lexema inteiro é consumido e vira **um** erro léxico. |
| **Palavra reservada** `[INT]`, `[IF]`, … | Mesmo padrão do identificador + consulta à tabela da §3 ao aceitar | `if`, `while`, `int`, `return` | Lista fechada de 15 palavras (§3), todas em minúsculas. `If`, `WHILE` são identificadores comuns. |
| **String** `[STRING_LITERAL]` | `'"' ( [^"\\\n\r] \| escape )* '"'` | `"ok"`, `"linha 1"`, `"a\"b"`, `""` | Escapes: `\n \t \" \' \\`. Outro escape (ex.: `\q`) → erro, mas a string é lida até o `"` final. **Não** há string multilinha: fim de linha ou EOF antes do `"` → erro "string não fechada". |
| **Operador** `[PLUS]`, `[EQ]`, … | `"+" \| "-" \| "*" \| "/" \| "%" \| "=" \| "==" \| "!=" \| "<" \| "<=" \| ">" \| ">=" \| "&&" \| "\|\|" \| "!"` | `=`, `==`, `<=`, `+`, `&&` | Formas compostas: `==`, `!=`, `<=`, `>=`, `&&`, `\|\|`. `&` e `\|` **sozinhos não existem** (erro léxico). Não há `++`, `--`, `+=`. |
| **Literal numérico** `[INT_LITERAL]`, `[DOUBLE_LITERAL]` | inteiro: `digito+`  ·  real: `digito+ "." digito+` | `10`, `0`, `007`, `3.14` | Sem sinal (o `-` é operador). Parte inteira e fracionária obrigatórias: `3.` e `.5` são inválidos. Sem notação científica nem hexadecimal. |

**Tokens auxiliares** (fora das cinco categorias, mas necessários ao escopo fixo):

| Token | Notação | Observação |
|---|---|---|
| Literal de char `[CHAR_LITERAL]` | `"'" ( [^'\\\n\r] \| escape ) "'"` | Exatamente um caractere: `'a'`, `'\n'`. `''` e `'ab'` são erros. Atende o tipo `char`. |
| Delimitadores | `"(" \| ")" \| "{" \| "}" \| ";" \| ","` | `[LPAREN] [RPAREN] [LBRACE] [RBRACE] [SEMICOLON] [COMMA]` |
| Literais lógicos | `true`, `false` | São palavras reservadas (§3); atendem o tipo `bool`. |
| Fim de arquivo `[EOF]` | — | Sempre o último token emitido. |

## 3. Palavras reservadas (lista fechada)

| Grupo | Palavras |
|---|---|
| Tipos básicos | `int` `double` `bool` `char` `string` `void` |
| Controle — seleção | `if` `else` |
| Controle — repetição | `while` `for` `break` `continue` |
| Funções | `return` |
| Literais lógicos | `true` `false` |

Total: **15 palavras**. Funções e procedimentos não têm palavra-chave própria de declaração: são declarados como `tipo nome ( parâmetros ) { ... }`, com `void` para procedimentos; `return;` retorna sem valor e `return expr;` retorna com valor.

## 4. Respostas explícitas

**4.1 Alfabeto de entrada.** Caracteres válidos em algum ponto de algum token:

- letras ASCII `a–z`, `A–Z`; dígitos `0–9`; `_` (só após a 1ª letra de um identificador);
- espaço em branco: espaço, tab (`\t`), `\n`, `\r`;
- símbolos: `+ - * / % = < > ! & | ( ) { } ; , . " ' \`
  - `.` só é válido **dentro** de um literal real; `\` só dentro de string/char.
- **Dentro** de strings, chars e comentários é aceito **qualquer** caractere (inclusive acentos), exceto quebra de linha em string/char.

```
Σ = [a-zA-Z] ∪ [0-9] ∪ { _ } ∪ { ' ', \t, \r, \n } ∪ { + - * / % = < > ! & | ( ) { } ; , . " ' \ }
```

Qualquer outro caractere fora desses contextos (ex.: `@`, `#`, `$`, `?`, `[`, `]`, `.` solto, `_` inicial, `é`) é **erro léxico "caractere inválido"**, reportado com linha:coluna; o scanner descarta aquele caractere e continua.

**4.2 Case-sensitivity.** A linguagem é **case-sensitive**, e identificadores e palavras reservadas seguem a **mesma** regra: `total` ≠ `Total`, e só a grafia em minúsculas é reservada (`while` é palavra-chave; `While` é identificador).

**4.3 Espaços em branco e comentários.** São separadores e **não geram token**.

- Espaço em branco: `espaco+` (espaço, `\t`, `\r`, `\n`). Cada `\n` incrementa a linha e a coluna volta a 1.
- Comentário de linha: `"//" [^\n]*` — vai até o fim da linha (ou EOF).
- Comentário de bloco: `"/*" ( [^*] | "*"+ [^*/] )* "*"+ "/"` — vai do `/*` até o **primeiro** `*/` e pode ocupar várias linhas (AFD: estados `BLOCK_COMMENT` e `BLOCK_STAR`).
- **Comentários de bloco não são aninhados**: o primeiro `*/` fecha o comentário. Em `/* a /* b */ c */`, o scanner produz `c`, `*`, `/`.
- `/*` sem `*/` até o EOF → erro léxico "comentário de bloco não fechado", apontando a linha:coluna do `/*`.

**4.4 Desambiguação — *maximal munch*.** O scanner sempre consome o **prefixo válido mais longo** antes de decidir o token:

| Lido | Próximo char | Token emitido |
|---|---|---|
| `=` | `=` | `==` `[EQ]`; senão `=` `[ASSIGN]` |
| `<` / `>` | `=` | `<=` `[LE]` / `>=` `[GE]`; senão `<` / `>` |
| `!` | `=` | `!=` `[NEQ]`; senão `!` `[NOT]` |
| `&` / `\|` | `&` / `\|` | `&&` / `\|\|`; senão **erro** |
| `/` | `/` ou `*` | início de comentário; senão `/` `[SLASH]` |
| letra/dígito | letra, dígito, `_` | continua o identificador (`ifx` é **um** identificador, não `if` + `x`) |
| dígitos | `.` | tenta literal real (`3.14`) |

Palavra reservada × identificador: primeiro se lê o identificador inteiro; só então se consulta a tabela da §3. Assim, `returnValor` é identificador.

**4.5 Palavras reservadas.** Exatamente as 15 da §3: `int, double, bool, char, string, void, if, else, while, for, return, break, continue, true, false`.

## 5. Erros léxicos e recuperação

| Situação | Mensagem | Onde o scanner retoma |
|---|---|---|
| Caractere fora do alfabeto | caractere inválido `'@'` | no caractere seguinte |
| String sem `"` antes do fim de linha | string não fechada (fim de linha) | no início da próxima linha |
| String sem `"` antes do EOF | string não fechada (fim de arquivo) | emite `[EOF]` |
| Escape inválido em string/char | sequência de escape inválida | após o `"`/`'` de fechamento |
| `/*` sem `*/` | comentário de bloco não fechado | emite `[EOF]` |
| `3.` (sem dígito após `.`) | literal numérico malformado | logo após o `.` |
| Identificador com > 31 caracteres | identificador com mais de 31 caracteres | após o identificador |
| `&` ou `\|` sozinho | operador inválido | no caractere seguinte |
| `''` ou `'ab'` | literal de char vazio / com mais de um caractere | após o `'` final |

Todo erro é reportado com **linha e coluna do início** do lexema, gera um token `[ERROR]` e **não interrompe** a análise.

## 6. Exemplo

```c
int soma(int a, int b) { return a + b; }  // função com retorno
void main() {
    double media = 7.5; bool ok = media >= 6.0;
    if (ok && soma(2, 3) != 0) { print("aprovado\n"); } else { x = 'F'; }
}
```

Linha 1 → `int` `soma` `(` `int` `a` `,` `int` `b` `)` `{` `return` `a` `+` `b` `;` `}` (o comentário é descartado). Note que `print` é identificador comum, não palavra reservada.
