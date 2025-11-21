import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsável pela Fase 4: Geração de Código Intermediário.
 * Converte a Árvore Sintática em um código linear de "Três Endereços" (TAC - Three Address Code)
 * ou uma variação simplificada para Máquina de Pilha/Registradores.
 * * Gerencia a alocação de registradores temporários (R1, R2...) e Labels de desvio (L1, L2...)
 * para controlar o fluxo de execução (IF/ELSE, WHILE).
 */
public class GeradorCodigoIntermediario {

    private List<String> codigo;        // Lista onde as instruções geradas são armazenadas sequencialmente
    private int contadorRegistrador;    // Contador para gerar nomes únicos de registradores (R1, R2...)
    private int contadorLabel;          // Contador para gerar labels únicos para saltos (L1, L2...)

    public GeradorCodigoIntermediario() {
        this.codigo = new ArrayList<>();
        this.contadorRegistrador = 1;
        this.contadorLabel = 1;
    }

    public List<String> getCodigo() {
        return codigo;
    }

    /**
     * Reinicia o contador de registradores.
     * Estratégia simplificada: assume que registradores podem ser reutilizados
     * entre comandos distintos (não há análise de vida útil complexa).
     */
    private void resetContadorRegistrador() {
        this.contadorRegistrador = 1;
    }

    /**
     * Gera um novo nome de registrador temporário (ex: R1, R2).
     */
    private String alocarRegistrador() {
        return "R" + (contadorRegistrador++);
    }

    /**
     * Gera um novo Label único para marcação de código (ex: L1, L2).
     */
    private String alocarLabel() {
        return "L" + (contadorLabel++);
    }

    /**
     * Adiciona uma instrução à lista final de código.
     */
    private void emitir(String instrucao) {
        codigo.add(instrucao);
    }

    /**
     * Método principal de varredura da árvore.
     * Despacha a geração para métodos específicos dependendo do tipo do nó.
     * @param no O nó atual da Árvore Sintática.
     */
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
                // Para nós que não geram código direto (ex: blocos), visita os filhos
                for (NoArvore filho : no.filhos) {
                    gerar(filho);
                }
                break;
        }
    }

    /**
     * Gera código para atribuições (ex: a = b + 1).
     * Estratégia:
     * 1. Reseta registradores (nova instrução).
     * 2. Calcula o valor da expressão do lado direito em um registrador temporário.
     * 3. Emite STORE para salvar esse valor na variável do lado esquerdo.
     */
    private void gerarAtribuicao(NoArvore noAtribuicao) {
        resetContadorRegistrador();

        // Filho 0 é o identificador da variável destino
        NoArvore noVar = noAtribuicao.filhos.get(0);
        String nomeVar = noVar.valor;

        // Filho 2 começa a expressão (após o token '=')
        String regResultado = gerarExpressao(noAtribuicao, 2);

        emitir("STORE " + nomeVar + ", " + regResultado);
    }

    /**
     * Gera código para estruturas condicionais (SE / ENTAO / SENAO).
     * Utiliza Labels para pular o bloco 'então' se a condição for falsa,
     * ou pular o bloco 'senão' ao final do 'então'.
     */
    private void gerarCondicional(NoArvore noCondicional) {
        resetContadorRegistrador();

        // Estrutura esperada da árvore: [se, Condicao, entao, Comando, (senao, Comando)?]
        NoArvore noCondicao = noCondicional.filhos.get(1);
        NoArvore noComandoEntao = noCondicional.filhos.get(3);

        String labelFim = alocarLabel(); // Label para onde ir se a condição falhar (ou fim do IF)

        // Gera código da condição.
        // Contrato: Se FALSO, pula para labelFim. Se VERDADEIRO, continua (fallthrough).
        gerarCodigoCondicao(noCondicao, null, labelFim);

        // --- Bloco ENTAO ---
        gerar(noComandoEntao);

        // Verifica se existe a parte SENAO
        if (noCondicional.filhos.size() > 5 && noCondicional.filhos.get(4).valor.equals("senao")) {
            String labelFinalReal = alocarLabel(); // Label para o fim absoluto da estrutura
            emitir("JMP " + labelFinalReal); // Terminou o 'entao', pula o 'senao'

            emitir("LABEL " + labelFim); // Aqui começa o bloco 'senao' (ponto de salto se condição falhou)

            // --- Bloco SENAO ---
            NoArvore noComandoSenao = noCondicional.filhos.get(5);
            gerar(noComandoSenao);

            emitir("LABEL " + labelFinalReal); // Ponto de encontro após o IF/ELSE completo
        } else {
            // Se não tem senao, o labelFim marca apenas o fim do bloco 'entao'
            emitir("LABEL " + labelFim);
        }
    }

    /**
     * Gera código para laços (ENQUANTO).
     * Cria um Label de início para o loop e um Label de fim para saída.
     */
    private void gerarIterativo(NoArvore noIterativo) {
        resetContadorRegistrador();
        String labelInicio = alocarLabel();
        String labelFim = alocarLabel();

        NoArvore noCondicao = noIterativo.filhos.get(1);
        NoArvore noComando = noIterativo.filhos.get(2);

        emitir("LABEL " + labelInicio); // Ponto de retorno do loop

        // Avalia a condição a cada iteração.
        // Se FALSO, pula para labelFim (sai do loop).
        gerarCodigoCondicao(noCondicao, null, labelFim);

        // Corpo do loop
        gerar(noComando);

        // Salto incondicional para reavaliar a condição
        emitir("JMP " + labelInicio);

        emitir("LABEL " + labelFim); // Ponto de saída
    }

    /**
     * Método recursivo complexo para gerar código de condições booleanas.
     * Lida com Curto-Circuito e lógica aninhada (AND, OR, NOT).
     * * @param no Nó atual da condição (pode ser E, OR, NOT ou CondicaoSimples).
     * @param labelTrue Label para pular se o resultado for VERDADEIRO (se null, segue fluxo).
     * @param labelFalse Label para pular se o resultado for FALSO (se null, segue fluxo).
     */
    private void gerarCodigoCondicao(NoArvore no, String labelTrue, String labelFalse) {

        // Verifica se há operadores lógicos (E / OR) nos filhos diretos
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
            // Simplificação: Assume que a esquerda é o primeiro filho lógico
            NoArvore esquerda = no.filhos.get(0);

            if (op.equals("E")) {
                // Lógica E (AND):
                // Se Esquerda falhar, já é Falso (vai para labelFalse).
                // Se Esquerda for True, precisa avaliar a Direita.
                gerarCodigoCondicao(esquerda, null, labelFalse);
                gerarCodigoCondicao(direita, labelTrue, labelFalse);

            } else if (op.equals("OR")) {
                // Lógica OR:
                // Se Esquerda for True, já é Verdadeiro (vai para labelTrue).
                // Se Esquerda for False, precisa avaliar a Direita.
                gerarCodigoCondicao(esquerda, labelTrue, null);
                gerarCodigoCondicao(direita, labelTrue, labelFalse);
            }
            return;
        }

        // --- CASO NOT ---
        for (NoArvore filho : no.filhos) {
            if (filho.valor.equals("NOT")) {
                // Estrutura: [ (, NOT, CondicaoInterna, ) ]
                NoArvore operando = no.filhos.get(2); // Pega a condição interna

                // INVERSÃO DE LÓGICA:
                // O label de sucesso do filho vira o label de falha do pai, e vice-versa.
                gerarCodigoCondicao(operando, labelFalse, labelTrue);
                return;
            }
        }

        // --- CASO CONDICAO SIMPLES (Base da recursão) ---
        NoArvore noSimples = null;

        // Localiza o nó 'CondicaoSimples' (pode ser o próprio nó ou um filho devido a parênteses)
        if (no.valor.equals("CondicaoSimples")) {
            noSimples = no;
        } else {
            for (NoArvore filho : no.filhos) {
                if (filho.valor.equals("CondicaoSimples")) {
                    noSimples = filho;
                    break;
                }
            }
        }

        if (noSimples != null) {
            // Estrutura: Termo1 OP Termo2
            NoArvore termo1 = noSimples.filhos.get(0);
            NoArvore op = noSimples.filhos.get(1);
            NoArvore termo2 = noSimples.filhos.get(2);

            // Carrega os valores em registradores
            String reg1 = carregarTermo(termo1);
            String reg2 = carregarTermo(termo2);
            String opMnem = traduzirOperadorLogico(op.valor);

            // Emite a instrução de comparação (ex: CMPGT R1, R2)
            // O resultado booleano fica armazenado no próprio R1 (convenção simplificada) ou flag
            emitir(opMnem + " " + reg1 + ", " + reg2);

            // Gera os saltos condicionais baseados nos labels solicitados
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

    /**
     * Gera código para expressões aritméticas (ex: a + b * 2).
     * Respeita a ordem da árvore (que já respeita precedência).
     * @param noPai Nó contendo a expressão.
     * @param indiceInicio Índice do primeiro filho que compõe a expressão.
     * @return O nome do registrador onde o resultado final ficou armazenado.
     */
    private String gerarExpressao(NoArvore noPai, int indiceInicio) {
        // Carrega o primeiro termo
        NoArvore primeiroTermoNo = noPai.filhos.get(indiceInicio).filhos.get(0);
        String regAtual = carregarTermo(primeiroTermoNo);

        // Itera sobre os pares (Operador, PróximoTermo)
        for (int i = indiceInicio + 1; i < noPai.filhos.size(); i += 2) {
            String op = noPai.filhos.get(i).valor;
            NoArvore proximoTermoNo = noPai.filhos.get(i + 1).filhos.get(0);

            // Otimização: Se o próximo termo for um número literal, usa instrução imediata (ex: ADDI)
            if (isNumero(proximoTermoNo.valor)) {
                String opImediato = traduzirOperadorAritmeticoImediato(op);
                emitir(opImediato + " " + regAtual + ", " + proximoTermoNo.valor);
            } else {
                // Se for variável, carrega em registrador e usa instrução padrão (ex: ADD)
                String regProximo = carregarTermo(proximoTermoNo);
                String opPadrao = traduzirOperadorAritmetico(op);
                // Formato: ADD R1, R1, R2 (Destino, Fonte1, Fonte2)
                emitir(opPadrao + " " + regAtual + ", " + regAtual + ", " + regProximo);
            }
        }
        return regAtual;
    }

    /**
     * Helper para carregar uma variável ou número em um registrador.
     */
    private String carregarTermo(NoArvore noTermo) {
        String reg = alocarRegistrador();
        if (isNumero(noTermo.valor)) {
            // Carregamento de literal
            emitir("LOADI " + reg + ", " + noTermo.valor);
        } else {
            // Carregamento de variável da memória
            emitir("LOAD " + reg + ", " + noTermo.valor);
        }
        return reg;
    }

    private boolean isNumero(String s) {
        return Character.isDigit(s.charAt(0));
    }

    /**
     * Traduz operadores lógicos do código fonte para mnemônicos assembly.
     */
    private String traduzirOperadorLogico(String op) {
        switch (op) {
            case ">":  return "CMPGT";  // Compare Greater Than
            case "<":  return "CMPLT";  // Compare Less Than
            case "==": return "CMPEQ";  // Compare Equal
            case ">=": return "CMPGE";  // Compare Greater Equal
            case "<=": return "CMPLE";  // Compare Less Equal
            case "!=": return "CMPNE";  // Compare Not Equal
            default: throw new RuntimeException("Op lógico inválido: " + op);
        }
    }

    /**
     * Traduz operadores aritméticos para mnemônicos assembly padrão.
     */
    private String traduzirOperadorAritmetico(String op) {
        switch (op) {
            case "+": return "ADD";
            case "-": return "SUB";
            case "*": return "MUL";
            case "/": return "DIV";
            case "RESTO": return "MOD";
            default: throw new RuntimeException("Op aritmético inválido: " + op);
        }
    }

    /**
     * Traduz operadores aritméticos para versões imediatas (quando o operando é um número fixo).
     */
    private String traduzirOperadorAritmeticoImediato(String op) {
        switch (op) {
            case "+": return "ADDI";
            case "-": return "SUBI";
            // Multiplicação e Divisão imediatas não suportadas nesta arquitetura simplificada,
            // cai no padrão que carregará o número em registrador antes.
            default: return traduzirOperadorAritmetico(op);
        }
    }
}