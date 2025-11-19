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
        // Todos os "new NoArvore(valor)" foram atualizados para "new NoArvore(valor, linha)"
        NoArvore noPrograma = new NoArvore("Programa", 0); 
        noPrograma.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha));
        consumir(TipoToken.INICIO_PROGRAMA);

        while (tokenAtual.tipo == TipoToken.TIPO_INTEIRO ||
               tokenAtual.tipo == TipoToken.TIPO_REAL ||
               tokenAtual.tipo == TipoToken.TIPO_CARACTER) {
            noPrograma.adicionarFilho(declaracaoTipo());
        }
        while (tokenAtual.tipo != TipoToken.FIM_PROGRAMA && tokenAtual.tipo != TipoToken.EOF) {
            noPrograma.adicionarFilho(comando());
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
    private NoArvore comando() {
        if (tokenAtual.tipo == TipoToken.SE) {
            return condicional();
        } else if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            return atribuicao();
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
        
        noAtribuicao.adicionarFilho(expressao());
        
        while (tokenAtual.tipo == TipoToken.OP_SOMA) {
            noAtribuicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // +
            consumir(tokenAtual.tipo);
            noAtribuicao.adicionarFilho(expressao());
        }
        
        consumir(TipoToken.PONTO_E_VIRGULA); 
        
        return noAtribuicao;
    }
    
    private NoArvore expressao() {
        NoArvore noExpressao = new NoArvore("Expressao", tokenAtual.linha);
        noExpressao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // id ou numero
        consumir(tokenAtual.tipo);
        return noExpressao;
    }
    
    private NoArvore condicional() {
        NoArvore noCondicional = new NoArvore("Condicional", tokenAtual.linha);
        
        noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // se
        consumir(TipoToken.SE);
        
        noCondicional.adicionarFilho(condicao());
        
        noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // entao
        consumir(TipoToken.ENTAO);
        
        noCondicional.adicionarFilho(comando());
        
        if (tokenAtual.tipo == TipoToken.SENAO) {
            noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // senao
            consumir(TipoToken.SENAO);
            noCondicional.adicionarFilho(comando());
        }
        
        noCondicional.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // ;
        consumir(TipoToken.PONTO_E_VIRGULA);
        
        return noCondicional;
    }
    
    private NoArvore condicao() {
        NoArvore noCondicao = new NoArvore("Condicao", tokenAtual.linha);

        noCondicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // (
        consumir(TipoToken.ABRE_PARENTESES);

        noCondicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // id
        consumir(TipoToken.IDENTIFICADOR);

        noCondicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // op_logico
        consumir(TipoToken.OP_LOGICO);
        
        noCondicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // id ou numero
        if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            consumir(TipoToken.IDENTIFICADOR);
        } else if (tokenAtual.tipo == TipoToken.NUMERO) {
            consumir(TipoToken.NUMERO);
        } else {
             throw new RuntimeException("Erro Sintático: Esperado identificador ou número na condição");
        }
        
        noCondicao.adicionarFilho(new NoArvore(tokenAtual.lexema, tokenAtual.linha)); // )
        consumir(TipoToken.FECHA_PARENTESES);
        
        return noCondicao;
    }
    
    private void iterativo(){
        //...
    }
}