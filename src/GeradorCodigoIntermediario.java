import java.util.ArrayList;
import java.util.List;

public class GeradorCodigoIntermediario {

    private List<String> codigo;
    private int contadorRegistrador;
    private int contadorLabel;

    public GeradorCodigoIntermediario() {
        this.codigo = new ArrayList<>();
        this.contadorRegistrador = 1;
        this.contadorLabel = 1;
    }

    public List<String> getCodigo() {
        return codigo;
    }

    private void resetContadorRegistrador() {
        this.contadorRegistrador = 1;
    }

    private String alocarRegistrador() {
        return "R" + (contadorRegistrador++);
    }

    private String alocarLabel() {
        return "L" + (contadorLabel++);
    }

    private void emitir(String instrucao) {
        codigo.add(instrucao);
    }

    public void gerar(NoArvore no) {
        if (no == null) return;

        switch (no.valor) {
            case "Iterativo":
                gerarIterativo(no);
                break;
            case "Atribuicao":
                gerarAtribuicao(no);
                break;
            case "Condicional":
                gerarCondicional(no);
                break;
            default:
                for (NoArvore filho : no.filhos) {
                    gerar(filho);
                }
                break;
        }
    }

    private void gerarAtribuicao(NoArvore noAtribuicao) {
        resetContadorRegistrador();
        NoArvore noVar = noAtribuicao.filhos.get(0);
        String nomeVar = noVar.valor;
        String regResultado = gerarExpressao(noAtribuicao, 2);
        emitir("STORE " + nomeVar + ", " + regResultado);
    }

    private void gerarCondicional(NoArvore noCondicional) {
        resetContadorRegistrador();

        // Estrutura: [se, Condicao, entao, Comando, ...]
        NoArvore noCondicao = noCondicional.filhos.get(1);
        NoArvore noComandoEntao = noCondicional.filhos.get(3);

        String labelFim = alocarLabel();

        // Gera código para a condição.
        // Contrato: Se FALSO, pula para labelFim. Se VERDADEIRO, cai no bloco (fallthrough).
        gerarCodigoCondicao(noCondicao, null, labelFim);

        // Bloco Entao
        gerar(noComandoEntao);

        // Verifica se tem Senao
        if (noCondicional.filhos.size() > 5 && noCondicional.filhos.get(4).valor.equals("senao")) {
            String labelFinalReal = alocarLabel();
            emitir("JMP " + labelFinalReal); // Terminou o 'entao', pula o 'senao'

            emitir("LABEL " + labelFim); // Aqui começa o bloco 'senao'
            NoArvore noComandoSenao = noCondicional.filhos.get(5);
            gerar(noComandoSenao);

            emitir("LABEL " + labelFinalReal);
        } else {
            emitir("LABEL " + labelFim);
        }
    }

    private void gerarIterativo(NoArvore noIterativo) {
        resetContadorRegistrador();
        String labelInicio = alocarLabel();
        String labelFim = alocarLabel();

        NoArvore noCondicao = noIterativo.filhos.get(1);
        NoArvore noComando = noIterativo.filhos.get(2);

        emitir("LABEL " + labelInicio);

        // Gera código para a condição.
        // Contrato: Se FALSO, sai do loop (pula para labelFim).
        gerarCodigoCondicao(noCondicao, null, labelFim);

        gerar(noComando);
        emitir("JMP " + labelInicio);
        emitir("LABEL " + labelFim);
    }

    /**
     * Método recursivo para gerar código de condições complexas (AND, OR, NOT).
     * @param no Nó da árvore (Condicao ou CondicaoSimples)
     * @param labelTrue Label para pular se Verdadeiro (se null, faz fallthrough)
     * @param labelFalse Label para pular se Falso (se null, faz fallthrough)
     */
    private void gerarCodigoCondicao(NoArvore no, String labelTrue, String labelFalse) {

        // Verifica se há operadores lógicos (E / OR) nos filhos diretos
        // A estrutura gerada é geralmente [Esquerda, OP, Direita] devido à recursão no Sintático

        int indexOp = -1;
        for (int i = 0; i < no.filhos.size(); i++) {
            String val = no.filhos.get(i).valor;
            if (val.equals("E") || val.equals("OR")) {
                indexOp = i;
                break;
            }
        }

        if (indexOp != -1) {
            // --- CASO COMPOSTO (AND / OR) ---
            String op = no.filhos.get(indexOp).valor;
            NoArvore direita = no.filhos.get(indexOp + 1);

            // O nó da esquerda é tudo antes do operador.
            // Se houver múltiplos filhos antes (ex: parenteses), precisamos tratar com cuidado.
            // Na sua árvore atual, a recursão "aninha" à direita, então a esquerda geralmente é
            // uma CondicaoSimples ou um bloco NOT que ocupa as primeiras posições.
            // Simplificação: Pegamos o primeiro filho lógico (índice 0) como esquerda.
            NoArvore esquerda = no.filhos.get(0);

            if (op.equals("E")) {
                // Esquerda E Direita
                // Se Esquerda for Falso, o resultado é Falso -> Pula para labelFalse.
                // Se Esquerda for Verdadeiro, avalia a Direita.
                gerarCodigoCondicao(esquerda, null, labelFalse);
                gerarCodigoCondicao(direita, labelTrue, labelFalse);

            } else if (op.equals("OR")) {
                // Esquerda OR Direita
                // Se Esquerda for Verdadeiro, o resultado é Verdadeiro -> Pula para labelTrue.
                // Se Esquerda for Falso, avalia a Direita.
                gerarCodigoCondicao(esquerda, labelTrue, null);
                gerarCodigoCondicao(direita, labelTrue, labelFalse);
            }
            return;
        }

        // --- CASO NOT ---
        // Verifica se contém o token "NOT"
        for (NoArvore filho : no.filhos) {
            if (filho.valor.equals("NOT")) {
                // Estrutura: [ (, NOT, CondicaoInterna, ) ]
                // O operando é a CondicaoInterna (filho índice 2 normalmente)
                NoArvore operando = no.filhos.get(2);

                // Inverte os labels: Se NOT(A) é True, então A é False.
                // Logo, passamos labelFalse como destino de sucesso de A, e vice-versa.
                gerarCodigoCondicao(operando, labelFalse, labelTrue);
                return;
            }
        }

        // --- CASO CONDICAO SIMPLES ---
        NoArvore noSimples = null;

        // Se o próprio nó já for a CondicaoSimples
        if (no.valor.equals("CondicaoSimples")) {
            noSimples = no;
        } else {
            // Procura nos filhos (pode estar na posição 0 ou 1 dependendo dos parênteses)
            for (NoArvore filho : no.filhos) {
                if (filho.valor.equals("CondicaoSimples")) {
                    noSimples = filho;
                    break;
                }
            }
        }

        if (noSimples != null) {
            // ... (código de geração: carregarTermo, emitir, JMP) ...
            // (Mantenha o restante do bloco igual ao anterior)
            NoArvore termo1 = noSimples.filhos.get(0);
            NoArvore op = noSimples.filhos.get(1);
            NoArvore termo2 = noSimples.filhos.get(2);

            String reg1 = carregarTermo(termo1);
            String reg2 = carregarTermo(termo2);
            String opMnem = traduzirOperadorLogico(op.valor);

            emitir(opMnem + " " + reg1 + ", " + reg2);

            if (labelTrue != null && labelFalse == null) {
                emitir("JMPTRUE " + reg1 + ", " + labelTrue);
            } else if (labelTrue == null && labelFalse != null) {
                emitir("JMPFALSE " + reg1 + ", " + labelFalse);
            } else if (labelTrue != null && labelFalse != null) {
                emitir("JMPTRUE " + reg1 + ", " + labelTrue);
                emitir("JMP " + labelFalse);
            }
        }
    }

    private String gerarExpressao(NoArvore noPai, int indiceInicio) {
        NoArvore primeiroTermoNo = noPai.filhos.get(indiceInicio).filhos.get(0);
        String regAtual = carregarTermo(primeiroTermoNo);

        for (int i = indiceInicio + 1; i < noPai.filhos.size(); i += 2) {
            String op = noPai.filhos.get(i).valor;
            NoArvore proximoTermoNo = noPai.filhos.get(i + 1).filhos.get(0);

            if (isNumero(proximoTermoNo.valor)) {
                String opImediato = traduzirOperadorAritmeticoImediato(op);
                emitir(opImediato + " " + regAtual + ", " + proximoTermoNo.valor);
            } else {
                String regProximo = carregarTermo(proximoTermoNo);
                String opPadrao = traduzirOperadorAritmetico(op);
                emitir(opPadrao + " " + regAtual + ", " + regAtual + ", " + regProximo);
            }
        }
        return regAtual;
    }

    private String carregarTermo(NoArvore noTermo) {
        String reg = alocarRegistrador();
        if (isNumero(noTermo.valor)) {
            emitir("LOADI " + reg + ", " + noTermo.valor);
        } else {
            emitir("LOAD " + reg + ", " + noTermo.valor);
        }
        return reg;
    }

    private boolean isNumero(String s) {
        return Character.isDigit(s.charAt(0));
    }

    private String traduzirOperadorLogico(String op) {
        switch (op) {
            case ">":  return "CMPGT";
            case "<":  return "CMPLT";
            case "==": return "CMPEQ";
            case ">=": return "CMPGE";
            case "<=": return "CMPLE";
            case "!=": return "CMPNE";
            default: throw new RuntimeException("Op lógico inválido: " + op);
        }
    }

    private String traduzirOperadorAritmetico(String op) {
        switch (op) {
            case "+": return "ADD";
            case "-": return "SUB"; // (Embora não usado na MLP padrão, mantido por segurança)
            case "*": return "MUL";
            case "/": return "DIV";
            case "RESTO": return "MOD";
            default: throw new RuntimeException("Op aritmético inválido: " + op);
        }
    }

    private String traduzirOperadorAritmeticoImediato(String op) {
        switch (op) {
            case "+": return "ADDI";
            case "-": return "SUBI";
            default: return traduzirOperadorAritmetico(op);
        }
    }
}