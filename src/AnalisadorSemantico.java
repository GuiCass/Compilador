/**
 * Responsável pela validação de regras de contexto que a gramática não captura.
 * Principais verificações:
 * 1. Uso de variáveis não declaradas.
 * 2. Incompatibilidade de tipos (ex: atribuir float a int).
 */
public class AnalisadorSemantico {

    private TabelaDeSimbolos tabela;

    public AnalisadorSemantico(TabelaDeSimbolos tabela) {
        this.tabela = tabela;
    }

    /**
     * Percorre a árvore sintática recursivamente verificando regras em nós específicos.
     */
    public void analisar(NoArvore no) {
        if (no == null) {
            return;
        }

        switch (no.valor) {
            case "Atribuicao":
                validarAtribuicao(no);
                break;
            case "CondicaoSimples":
                validarCondicaoSimples(no);
                break;
        }

        for (NoArvore filho : no.filhos) {
            analisar(filho);
        }
    }

    /**
     * Valida atribuição: Variável alvo deve existir e ter tipo compatível com a expressão.
     */
    private void validarAtribuicao(NoArvore noAtribuicao) {
        NoArvore noVar = noAtribuicao.filhos.get(0);
        String varNome = noVar.valor;
        int linha = noVar.linha;

        // Verifica se a variável foi declarada
        String tipoVar = tabela.verificarDeclarada(varNome, linha);

        // Determina o tipo resultante da expressão à direita
        String tipoExpr = determinarTipoExpressao(noAtribuicao, 2);

        // Verifica compatibilidade estrita de tipos
        if (!tipoVar.equals(tipoExpr)) {
            throw new RuntimeException("Erro Semântico: Tipos incompatíveis na atribuição. " +
                    "Variável '" + varNome + "' (" + tipoVar + ") " +
                    "recebendo (" + tipoExpr + "). Linha " + linha);
        }
    }

    /**
     * Valida condições: Os termos comparados devem ser do mesmo tipo.
     */
    private void validarCondicaoSimples(NoArvore noCondicaoSimples) {
        NoArvore noTermo1 = noCondicaoSimples.filhos.get(0);
        NoArvore noTermo2 = noCondicaoSimples.filhos.get(2);

        String tipoTermo1 = determinarTipoTermo(noTermo1);
        String tipoTermo2 = determinarTipoTermo(noTermo2);

        if (!tipoTermo1.equals(tipoTermo2)) {
            throw new RuntimeException("Erro Semântico: Tipos incompatíveis na condição. " +
                    "Comparando (" + tipoTermo1 + ") com (" + tipoTermo2 + "). " +
                    "Linha " + noTermo1.linha);
        }
    }

    /**
     * Calcula o tipo resultante de uma expressão aritmética composta.
     */
    private String determinarTipoExpressao(NoArvore noPai, int indiceInicio) {
        NoArvore primeiroTermo = noPai.filhos.get(indiceInicio);
        String tipoResultante = determinarTipoTermo(primeiroTermo.filhos.get(0));

        // Verifica cada termo subsequente na expressão
        for (int i = indiceInicio + 2; i < noPai.filhos.size(); i += 2) {
            NoArvore proximoTermo = noPai.filhos.get(i);
            String tipoProximo = determinarTipoTermo(proximoTermo.filhos.get(0));

            if (!tipoResultante.equals(tipoProximo)) {
                throw new RuntimeException("Erro Semântico: Tipos incompatíveis na expressão. " +
                        "Operação entre (" + tipoResultante + ") e (" + tipoProximo + "). " +
                        "Linha " + proximoTermo.linha);
            }
        }
        return tipoResultante;
    }

    /**
     * Retorna o tipo de um termo simples (variável ou literal numérico).
     */
    private String determinarTipoTermo(NoArvore noTermo) {
        String valor = noTermo.valor;
        int linha = noTermo.linha;

        // Identifica literais
        if (Character.isDigit(valor.charAt(0))) {
            if (valor.contains(".")) {
                return "real";
            }
            return "inteiro";
        }

        // Busca o tipo da variável na tabela
        return tabela.verificarDeclarada(valor, linha);
    }
}