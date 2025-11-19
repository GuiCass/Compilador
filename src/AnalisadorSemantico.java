public class AnalisadorSemantico {

    private TabelaDeSimbolos tabela;

    public AnalisadorSemantico(TabelaDeSimbolos tabela) {
        this.tabela = tabela;
    }

    /**
     * Método principal que inicia a varredura da Árvore Sintática.
     */
    public void analisar(NoArvore no) {
        if (no == null) {
            return;
        }

        // Usamos o 'valor' do nó para decidir qual regra semântica aplicar
        switch (no.valor) {
            case "Atribuicao":
                validarAtribuicao(no);
                break;
            case "Condicao":
                validarCondicao(no);
                break;
            // "DeclaracaoTipo" não é necessária aqui, pois já foi processada
            // e populou a tabela durante a análise sintática.
        }

        // Continua a varredura recursivamente para os filhos
        for (NoArvore filho : no.filhos) {
            analisar(filho);
        }
    }

    /**
     * Valida um nó de Atribuição (id = expr)
     * Regra A: Verifica se a variável à esquerda foi declarada. 
     * Regra B: Verifica se os tipos são compatíveis. [cite: 32]
     */
    private void validarAtribuicao(NoArvore noAtribuicao) {
        // O primeiro filho é o ID (ex: "b")
        NoArvore noVar = noAtribuicao.filhos.get(0);
        String varNome = noVar.valor;
        int linha = noVar.linha;

        // Regra A: Verifica se 'b' foi declarado
        String tipoVar = tabela.verificarDeclarada(varNome, linha); // 

        // O terceiro filho (índice 2) é a primeira 'Expressao' (ex: "b + 1")
        // Vamos checar o tipo de toda a expressão da direita
        String tipoExpr = determinarTipoExpressao(noAtribuicao, 2);

        // Regra B: Verifica compatibilidade (ex: inteiro = inteiro)
        // (Simplificação: não estamos fazendo coerção de tipo, ex: real = inteiro)
        if (!tipoVar.equals(tipoExpr)) {
            throw new RuntimeException("Erro Semântico: Tipos incompatíveis na atribuição. " +
                    "Variável '" + varNome + "' (" + tipoVar + ") " +
                    "recebendo (" + tipoExpr + "). Linha " + linha); // [cite: 32]
        }
    }
    
    /**
     * Valida um nó de Condição (id op id/num)
     * Regra A: Verifica se os identificadores usados foram declarados. 
     * Regra C: Verifica se os tipos são compatíveis para a comparação. [cite: 33]
     */
    private void validarCondicao(NoArvore noCondicao) {
        // Filho 1: (
        // Filho 2: id
        // Filho 3: op
        // Filho 4: id/num
        // Filho 5: )
        NoArvore noTermo1 = noCondicao.filhos.get(1);
        NoArvore noTermo2 = noCondicao.filhos.get(3);
        
        String tipoTermo1 = determinarTipoTermo(noTermo1);
        String tipoTermo2 = determinarTipoTermo(noTermo2);
        
        // Regra C: Verifica compatibilidade (ex: inteiro > inteiro)
        if (!tipoTermo1.equals(tipoTermo2)) {
            throw new RuntimeException("Erro Semântico: Tipos incompatíveis na condição. " +
                    "Comparando (" + tipoTermo1 + ") com (" + tipoTermo2 + "). " +
                    "Linha " + noTermo1.linha); 
        }
    }

    /**
     * Determina o tipo de uma expressão complexa (ex: b + 1 + c)
     * A estrutura na AST é: [Expressao(b), +, Expressao(1), +, Expressao(c)]
     */
    private String determinarTipoExpressao(NoArvore noPai, int indiceInicio) {
        // Pega o tipo do primeiro termo
        NoArvore primeiroTermo = noPai.filhos.get(indiceInicio);
        String tipoResultante = determinarTipoTermo(primeiroTermo.filhos.get(0));

        // Itera sobre os pares (operador, termo) restantes
        for (int i = indiceInicio + 2; i < noPai.filhos.size(); i += 2) {
            NoArvore proximoTermo = noPai.filhos.get(i);
            String tipoProximo = determinarTipoTermo(proximoTermo.filhos.get(0));
            
            // Regra C: Verifica se os tipos na operação são compatíveis
            if (!tipoResultante.equals(tipoProximo)) {
                throw new RuntimeException("Erro Semântico: Tipos incompatíveis na expressão. " +
                    "Operação entre (" + tipoResultante + ") e (" + tipoProximo + "). " +
                    "Linha " + proximoTermo.linha); 
            }
            // (Lógica mais complexa lidaria com promoção, ex: int + real = real)
        }
        return tipoResultante;
    }

    /**
     * Determina o tipo de um único termo (que é filho de um nó 'Expressao').
     * Pode ser um ID (variável) ou um literal (número).
     */
    private String determinarTipoTermo(NoArvore noTermo) {
        String valor = noTermo.valor;
        int linha = noTermo.linha;

        // Se o lexema começa com dígito, é um número
        // (Simplificação. O léxico já deve ter classificado como NUMERO)
        if (Character.isDigit(valor.charAt(0))) {
            if (valor.contains(".")) {
                return "real";
            }
            return "inteiro";
        }

        // Se não for número, deve ser um ID.
        // Regra A: Verifica se foi declarado 
        return tabela.verificarDeclarada(valor, linha);
    }
}