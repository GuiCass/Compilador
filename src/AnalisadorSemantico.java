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
            case "CondicaoSimples": // CORREÇÃO: Valida apenas a folha da condição (a comparação)
                validarCondicaoSimples(no);
                break;
            // Casos como "Condicao", "NOT", "E" apenas propagam a análise para os filhos
        }

        // Continua a varredura recursivamente para os filhos
        for (NoArvore filho : no.filhos) {
            analisar(filho);
        }
    }

    /**
     * Valida um nó de Atribuição (id = expr)
     */
    private void validarAtribuicao(NoArvore noAtribuicao) {
        // O primeiro filho é o ID (ex: "b")
        NoArvore noVar = noAtribuicao.filhos.get(0);
        String varNome = noVar.valor;
        int linha = noVar.linha;

        // Regra A: Verifica se 'b' foi declarado
        String tipoVar = tabela.verificarDeclarada(varNome, linha);

        // O terceiro filho (índice 2) é a primeira 'Expressao'
        // Vamos checar o tipo de toda a expressão da direita
        String tipoExpr = determinarTipoExpressao(noAtribuicao, 2);

        // Regra B: Verifica compatibilidade
        if (!tipoVar.equals(tipoExpr)) {
            throw new RuntimeException("Erro Semântico: Tipos incompatíveis na atribuição. " +
                    "Variável '" + varNome + "' (" + tipoVar + ") " +
                    "recebendo (" + tipoExpr + "). Linha " + linha);
        }
    }

    /**
     * Valida um nó de Condição Simples: [ID, OP, ID/NUM]
     * CORREÇÃO: Adaptado para os índices da nova estrutura gerada pelo Sintático
     */
    private void validarCondicaoSimples(NoArvore noCondicaoSimples) {
        // Estrutura: Filho 0=ID, Filho 1=OP, Filho 2=ID/NUM
        NoArvore noTermo1 = noCondicaoSimples.filhos.get(0);
        NoArvore noTermo2 = noCondicaoSimples.filhos.get(2);

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
        }
        return tipoResultante;
    }

    /**
     * Determina o tipo de um único termo.
     */
    private String determinarTipoTermo(NoArvore noTermo) {
        String valor = noTermo.valor;
        int linha = noTermo.linha;

        // Se for número (começa com dígito)
        if (Character.isDigit(valor.charAt(0))) {
            if (valor.contains(".")) {
                return "real";
            }
            return "inteiro";
        }

        // Se não for número, deve ser um ID.
        return tabela.verificarDeclarada(valor, linha);
    }
}