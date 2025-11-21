/**
 * Implementa um Analisador Descendente Recursivo (Recursive Descent Parser).
 * Responsável por verificar se a sequência de tokens obedece à gramática da linguagem.
 * Também constrói a Árvore Sintática e popula a Tabela de Símbolos.
 */
public class AnalisadorSintatico {

    private AnalisadorLexico lexico;
    private Token tokenAtual;

    // Tabela de símbolos populada durante as declarações para uso posterior na análise semântica
    private TabelaDeSimbolos tabelaDeSimbolos = new TabelaDeSimbolos();

    public AnalisadorSintatico(AnalisadorLexico lexico) {
        this.lexico = lexico;
        this.tabelaDeSimbolos = new TabelaDeSimbolos();
        // Carrega o primeiro token para iniciar a análise ("Lookahead")
        this.tokenAtual = lexico.proximoToken();
    }

    public TabelaDeSimbolos getTabelaDeSimbolos() {
        return this.tabelaDeSimbolos;
    }

    /**
     * Compara o token atual com o tipo esperado. Se casar, avança para o próximo token.
     * Caso contrário, lança um erro sintático.
     */
    private void consumir(TipoToken tipoEsperado) {
        if (tokenAtual.tipo == tipoEsperado) {
            tokenAtual = lexico.proximoToken();
        } else {
            throw new RuntimeException("Erro Sintático: Esperado " + tipoEsperado +
                    " mas encontrado " + tokenAtual.tipo +
                    " na linha " + tokenAtual.linha);
        }
    }

    /**
     * Regra inicial da gramática:
     * Programa -> '$' Declaracoes* Comandos* '$.'
     */
    public NoArvore programa() {
        NoArvore noPrograma = new NoArvore("Programa", 0);
        noPrograma.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.INICIO_PROGRAMA);

        // Processa as declarações de variáveis (inteiro, real, caracter)
        while (tokenAtual.tipo == TipoToken.TIPO_INTEIRO ||
                tokenAtual.tipo == TipoToken.TIPO_REAL ||
                tokenAtual.tipo == TipoToken.TIPO_CARACTER) {
            noPrograma.adicionarFilho(declaracaoTipo());
        }

        // Processa a lista de comandos
        while (tokenAtual.tipo != TipoToken.FIM_PROGRAMA && tokenAtual.tipo != TipoToken.EOF) {
            // Inicia a contagem de profundidade em 0 para validar a Premissa 2
            noPrograma.adicionarFilho(comando(0));
        }

        noPrograma.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.FIM_PROGRAMA);

        System.out.println("Análise sintática concluída com sucesso!");
        return noPrograma;
    }

    /**
     * Regra: Tipo -> ('inteiro'|'real'|'caracter') ID (',' ID)* ';'
     * Também realiza a inserção dos identificadores na Tabela de Símbolos.
     */
    private NoArvore declaracaoTipo() {
        NoArvore noTipo = new NoArvore("DeclaracaoTipo", tokenAtual.linha);

        String tipoVariavel = tokenAtual.lexema;
        noTipo.adicionarFilho(new NoArvore(tipoVariavel, tokenAtual.linha));
        consumir(tokenAtual.tipo);

        // Declaração da primeira variável
        Token idToken = tokenAtual;
        tabelaDeSimbolos.declarar(idToken.lexema, tipoVariavel, idToken.linha);
        noTipo.adicionarFilho(new NoArvore(idToken.lexema, idToken.linha));
        consumir(TipoToken.IDENTIFICADOR);

        // Processa variáveis adicionais separadas por vírgula
        while (tokenAtual.tipo == TipoToken.VIRGULA) {
            noTipo.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
            consumir(TipoToken.VIRGULA);

            idToken = tokenAtual;
            tabelaDeSimbolos.declarar(idToken.lexema, tipoVariavel, idToken.linha);
            noTipo.adicionarFilho(new NoArvore(idToken.lexema, idToken.linha));
            consumir(TipoToken.IDENTIFICADOR);
        }

        noTipo.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.PONTO_E_VIRGULA);

        return noTipo;
    }

    /**
     * Regra: Comando -> Condicional | Iterativo | Atribuicao
     * Valida a profundidade máxima de aninhamento (Premissa 2).
     */
    private NoArvore comando(int profundidade) {
        if (profundidade > 10) {
            throw new RuntimeException("Erro Sintático: Profundidade máxima de 10 comandos excedida na linha " + tokenAtual.linha);
        }

        if (tokenAtual.tipo == TipoToken.SE) {
            return condicional(profundidade);
        } else if (tokenAtual.tipo == TipoToken.ENQUANTO) {
            return iterativo(profundidade);
        } else if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            return atribuicao();
        }
        return new NoArvore("ComandoVazio(ε)", tokenAtual.linha);
    }

    /**
     * Regra: Atribuicao -> ID '=' Expressao ';'
     */
    private NoArvore atribuicao() {
        NoArvore noAtribuicao = new NoArvore("Atribuicao", tokenAtual.linha);
        noAtribuicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // ID
        consumir(TipoToken.IDENTIFICADOR);

        noAtribuicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // =
        consumir(TipoToken.OP_ATRIBUICAO);

        // Processa a expressão (pode ser composta por múltiplos termos e operações)
        noAtribuicao.adicionarFilho(expressao());

        while (tokenAtual.tipo == TipoToken.OP_SOMA ||
                tokenAtual.tipo == TipoToken.OP_MULT ||
                tokenAtual.tipo == TipoToken.OP_DIV  ||
                tokenAtual.tipo == TipoToken.OP_RESTO) {

            noAtribuicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // Operador
            consumir(tokenAtual.tipo);

            noAtribuicao.adicionarFilho(expressao());
        }

        consumir(TipoToken.PONTO_E_VIRGULA);
        return noAtribuicao;
    }

    /**
     * Regra: Expressao -> Termo | Termo OP Termo | (Expressao)
     * Trata precedência básica através de parênteses.
     */
    private NoArvore expressao() {
        NoArvore noExpressao = new NoArvore("Expressao", tokenAtual.linha);

        if (tokenAtual.tipo == TipoToken.ABRE_PARENTESES) {
            consumir(TipoToken.ABRE_PARENTESES);
            noExpressao.adicionarFilho(expressao());

            // Na gramática simplificada, espera-se um operador entre expressões parentizadas
            if (tokenAtual.tipo == TipoToken.OP_SOMA || tokenAtual.tipo == TipoToken.OP_MULT ||
                    tokenAtual.tipo == TipoToken.OP_DIV || tokenAtual.tipo == TipoToken.OP_RESTO) {
                noExpressao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
                consumir(tokenAtual.tipo);
            }

            noExpressao.adicionarFilho(expressao());
            consumir(TipoToken.FECHA_PARENTESES);

        } else if (tokenAtual.tipo == TipoToken.NUMERO) {
            noExpressao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
            consumir(TipoToken.NUMERO);
        } else if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            noExpressao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
            consumir(TipoToken.IDENTIFICADOR);
        } else {
            throw new RuntimeException("Erro Sintático: Esperado número, ID ou '(' na linha " + tokenAtual.linha);
        }

        return noExpressao;
    }

    /**
     * Regra: Condicional -> 'se' Condicao 'entao' Comando ['senao' Comando]
     * Incrementa a profundidade ao chamar o próximo comando recursivamente.
     */
    private NoArvore condicional(int profundidade) {
        NoArvore noCondicional = new NoArvore("Condicional", tokenAtual.linha);

        noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.SE);

        noCondicional.adicionarFilho(condicao());

        noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.ENTAO);

        // Corpo do IF (aumenta profundidade)
        noCondicional.adicionarFilho(comando(profundidade + 1));

        if (tokenAtual.tipo == TipoToken.SENAO) {
            noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
            consumir(TipoToken.SENAO);
            // Corpo do ELSE (aumenta profundidade)
            noCondicional.adicionarFilho(comando(profundidade + 1));
        }

        return noCondicional;
    }

    /**
     * Regra: Condicao -> '(' CondicaoSimples ')' | '(' NOT Condicao ')'
     * Suporta recursão para operadores lógicos (E / OR).
     */
    private NoArvore condicao() {
        NoArvore noCondicao = new NoArvore("Condicao", tokenAtual.linha);

        if (tokenAtual.tipo == TipoToken.ABRE_PARENTESES) {
            consumir(TipoToken.ABRE_PARENTESES);
            noCondicao.adicionarFilho(new NoArvore("(", tokenAtual.linha));

            if (tokenAtual.tipo == TipoToken.ABRE_PARENTESES) {
                // Condição aninhada
                noCondicao.adicionarFilho(condicao());
            }
            else if (tokenAtual.tipo == TipoToken.OP_BOOLEANO_NOT) {
                // Operador Unário NOT
                noCondicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
                consumir(TipoToken.OP_BOOLEANO_NOT);
                noCondicao.adicionarFilho(condicao());

            } else {
                // Condição relacional padrão (ex: a > b)
                if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
                    NoArvore noSimples = new NoArvore("CondicaoSimples", tokenAtual.linha);

                    noSimples.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // ID
                    consumir(TipoToken.IDENTIFICADOR);

                    noSimples.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // OP Relacional
                    consumir(TipoToken.OP_LOGICO);

                    if (tokenAtual.tipo == TipoToken.IDENTIFICADOR || tokenAtual.tipo == TipoToken.NUMERO) {
                        noSimples.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
                        consumir(tokenAtual.tipo);
                    } else {
                        throw new RuntimeException("Erro Sintático: Esperado valor após operador lógico.");
                    }
                    noCondicao.adicionarFilho(noSimples);
                } else {
                    throw new RuntimeException("Erro Sintático: Esperado IDENTIFICADOR ou NOT após '('.");
                }
            }

            if (tokenAtual.tipo == TipoToken.FECHA_PARENTESES) {
                noCondicao.adicionarFilho(new NoArvore(")", tokenAtual.linha));
                consumir(TipoToken.FECHA_PARENTESES);
            } else {
                throw new RuntimeException("Erro Sintático: Esperado ')' final.");
            }

        } else {
            throw new RuntimeException("Erro Sintático: Condição deve começar com '('.");
        }

        // Suporte a condições compostas (E / OR)
        while (tokenAtual.tipo == TipoToken.OP_BOOLEANO_E || tokenAtual.tipo == TipoToken.OP_BOOLEANO_OR) {
            noCondicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
            consumir(tokenAtual.tipo);
            noCondicao.adicionarFilho(condicao());
        }

        return noCondicao;
    }

    /**
     * Regra: Iterativo -> 'enquanto' Condicao Comando
     */
    private NoArvore iterativo(int profundidade) {
        NoArvore noIterativo = new NoArvore("Iterativo", tokenAtual.linha);

        noIterativo.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.ENQUANTO);

        noIterativo.adicionarFilho(condicao());

        // Corpo do Loop (aumenta profundidade)
        noIterativo.adicionarFilho(comando(profundidade + 1));

        return noIterativo;
    }
}