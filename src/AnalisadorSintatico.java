public class AnalisadorSintatico {
	
    private AnalisadorLexico lexico;
    private Token tokenAtual;
    
    // A Tabela de Símbolos será usada para a análise semântica
    private TabelaDeSimbolos tabelaDeSimbolos = new TabelaDeSimbolos();

    public AnalisadorSintatico(AnalisadorLexico lexico) {
        this.lexico = lexico;
        this.tabelaDeSimbolos = new TabelaDeSimbolos(); 
        this.tokenAtual = lexico.proximoToken(); 
    }
    
    public TabelaDeSimbolos getTabelaDeSimbolos() { 
        return this.tabelaDeSimbolos;
    }
    
    // Método para consumir um token esperado e avançar para o próximo
    private void consumir(TipoToken tipoEsperado) {
        if (tokenAtual.tipo == tipoEsperado) {
            tokenAtual = lexico.proximoToken();
        } else {
            throw new RuntimeException("Erro Sintático: Esperado " + tipoEsperado + 
                                       " mas encontrado " + tokenAtual.tipo + 
                                       " na linha " + tokenAtual.linha);
        }
    }

    // Método inicial, baseado na regra: inicio = '$' tipo* comando* '$.'
    public NoArvore programa() {
        NoArvore noPrograma = new NoArvore("Programa", 0);
        noPrograma.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.INICIO_PROGRAMA);

        while (tokenAtual.tipo == TipoToken.TIPO_INTEIRO ||
                tokenAtual.tipo == TipoToken.TIPO_REAL ||
                tokenAtual.tipo == TipoToken.TIPO_CARACTER) {
            noPrograma.adicionarFilho(declaracaoTipo());
        }

        while (tokenAtual.tipo != TipoToken.FIM_PROGRAMA && tokenAtual.tipo != TipoToken.EOF) {
            // CORREÇÃO: Passando 0 como profundidade inicial
            noPrograma.adicionarFilho(comando(0));
        }

        noPrograma.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.FIM_PROGRAMA);

        System.out.println("Análise sintática concluída com sucesso!");
        return noPrograma;
    }

    // regra: tipo = ('inteiro'|'real'|'caracter') identificador (',' identificador)* ';'
    private NoArvore declaracaoTipo() {
        NoArvore noTipo = new NoArvore("DeclaracaoTipo", tokenAtual.linha);
        
        String tipoVariavel = tokenAtual.lexema; // Salva o TIPO (ex: "inteiro")
        noTipo.adicionarFilho(new NoArvore(tipoVariavel, tokenAtual.linha));
        consumir(tokenAtual.tipo); 

        // --- LÓGICA SEMÂNTICA ---
        Token idToken = tokenAtual;
        tabelaDeSimbolos.declarar(idToken.lexema, tipoVariavel, idToken.linha);
        // -----------------------------------
        noTipo.adicionarFilho(new NoArvore(idToken.lexema, idToken.linha));
        consumir(TipoToken.IDENTIFICADOR);

        while (tokenAtual.tipo == TipoToken.VIRGULA) {
            noTipo.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
            consumir(TipoToken.VIRGULA);
            
            // --- LÓGICA SEMÂNTICA ---
            idToken = tokenAtual;
            tabelaDeSimbolos.declarar(idToken.lexema, tipoVariavel, idToken.linha);
            // -----------------------------------
            noTipo.adicionarFilho(new NoArvore(idToken.lexema, idToken.linha));
            consumir(TipoToken.IDENTIFICADOR);
        }
        
        noTipo.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.PONTO_E_VIRGULA);
        
        return noTipo;
    }

    // regra: comando = condicional | iterativo | atribuição | ε
    private NoArvore comando(int profundidade) {
        // Validação da Premissa 2: Profundidade máxima de 10
        if (profundidade > 10) {
            throw new RuntimeException("Erro Sintático: Profundidade máxima de 10 comandos excedida na linha " + tokenAtual.linha);
        }

        if (tokenAtual.tipo == TipoToken.SE) {
            return condicional(profundidade); // Repassa a profundidade atual
        } else if (tokenAtual.tipo == TipoToken.ENQUANTO) {
            return iterativo(profundidade);   // Repassa a profundidade atual
        } else if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            return atribuicao(); // Atribuição não aumenta profundidade, é folha
        }
        return new NoArvore("ComandoVazio(ε)", tokenAtual.linha);
    }

    // regra: atribuição = identificador '=' (expressão | identificador) ... ';'
    private NoArvore atribuicao() {
        NoArvore noAtribuicao = new NoArvore("Atribuicao", tokenAtual.linha);
        noAtribuicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // id
        consumir(TipoToken.IDENTIFICADOR);

        noAtribuicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // =
        consumir(TipoToken.OP_ATRIBUICAO);

        // Primeiro termo
        noAtribuicao.adicionarFilho(expressao());

        // Loop para operadores (+, *, /, RESTO)
        while (tokenAtual.tipo == TipoToken.OP_SOMA ||
                tokenAtual.tipo == TipoToken.OP_MULT ||
                tokenAtual.tipo == TipoToken.OP_DIV  ||
                tokenAtual.tipo == TipoToken.OP_RESTO) {

            noAtribuicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // op
            consumir(tokenAtual.tipo);

            noAtribuicao.adicionarFilho(expressao());
        }

        consumir(TipoToken.PONTO_E_VIRGULA);
        return noAtribuicao;
    }

    private NoArvore expressao() {
        NoArvore noExpressao = new NoArvore("Expressao", tokenAtual.linha);

        if (tokenAtual.tipo == TipoToken.ABRE_PARENTESES) {
            // Tratamento de parênteses: '(' expressão operador expressão ')'
            consumir(TipoToken.ABRE_PARENTESES);
            noExpressao.adicionarFilho(expressao()); // Recursão para o lado esquerdo

            // Operador obrigatório dentro dos parênteses segundo a regra estrita
            if (tokenAtual.tipo == TipoToken.OP_SOMA || tokenAtual.tipo == TipoToken.OP_MULT ||
                    tokenAtual.tipo == TipoToken.OP_DIV || tokenAtual.tipo == TipoToken.OP_RESTO) {
                noExpressao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
                consumir(tokenAtual.tipo);
            }

            noExpressao.adicionarFilho(expressao()); // Recursão para o lado direito
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

    private NoArvore condicional(int profundidade) {
        NoArvore noCondicional = new NoArvore("Condicional", tokenAtual.linha);

        noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // se
        consumir(TipoToken.SE);

        noCondicional.adicionarFilho(condicao());

        noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // entao
        consumir(TipoToken.ENTAO);

        // CORREÇÃO: Incrementa profundidade para o comando interno
        noCondicional.adicionarFilho(comando(profundidade + 1));

        if (tokenAtual.tipo == TipoToken.SENAO) {
            noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // senao
            consumir(TipoToken.SENAO);
            // CORREÇÃO: Incrementa profundidade para o comando do senão
            noCondicional.adicionarFilho(comando(profundidade + 1));
        }

        // Lembrar: removemos o ponto-e-vírgula aqui no passo anterior

        return noCondicional;
    }

    // regra: condicao = condicaoSimples ( (E | OR) condicaoSimples )*
    private NoArvore condicao() {
        NoArvore noCondicao = new NoArvore("Condicao", tokenAtual.linha);

        // Processa a primeira condição simples: ( id op id )
        noCondicao.adicionarFilho(condicaoSimples());

        // Verifica se há conectivos lógicos E / OR
        while (tokenAtual.tipo == TipoToken.OP_BOOLEANO_E || tokenAtual.tipo == TipoToken.OP_BOOLEANO_OR) {
            noCondicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // E / OR
            consumir(tokenAtual.tipo);

            noCondicao.adicionarFilho(condicaoSimples());
        }

        return noCondicao;
    }

    // Método auxiliar para a parte básica: '(' id op id ')'
    private NoArvore condicaoSimples() {
        NoArvore noSimples = new NoArvore("CondicaoSimples", tokenAtual.linha);

        consumir(TipoToken.ABRE_PARENTESES);

        // Lado esquerdo
        if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            noSimples.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
            consumir(TipoToken.IDENTIFICADOR);
        } else {
            // Permitir número à esquerda também? A regra diz 'identificador' primeiro, mas lógica comum permite num.
            // Seguindo a regra estrita:
            throw new RuntimeException("Erro Sintático: Esperado Identificador no início da condição.");
        }

        noSimples.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // op relacional (> < ==)
        consumir(TipoToken.OP_LOGICO);

        // Lado direito (id ou numero)
        if (tokenAtual.tipo == TipoToken.IDENTIFICADOR || tokenAtual.tipo == TipoToken.NUMERO) {
            noSimples.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
            consumir(tokenAtual.tipo);
        } else {
            throw new RuntimeException("Erro Sintático: Esperado valor na comparação.");
        }

        consumir(TipoToken.FECHA_PARENTESES);
        return noSimples;
    }

    private NoArvore iterativo(int profundidade) {
        NoArvore noIterativo = new NoArvore("Iterativo", tokenAtual.linha);

        noIterativo.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // enquanto
        consumir(TipoToken.ENQUANTO);

        noIterativo.adicionarFilho(condicao());

        // CORREÇÃO: Incrementa profundidade para o corpo do loop
        noIterativo.adicionarFilho(comando(profundidade + 1));

        return noIterativo;
    }
}