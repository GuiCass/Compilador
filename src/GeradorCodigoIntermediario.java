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

    /**
     * Retorna a lista de instruções geradas.
     */
    public List<String> getCodigo() {
        return codigo;
    }

    /**
     * Zera os registradores temporários (usado por novas instruções).
     */
    private void resetContadorRegistrador() {
        this.contadorRegistrador = 1;
    }

    /**
     * Aloca um novo registrador temporário (ex: "R1", "R2").
     */
    private String alocarRegistrador() {
        return "R" + (contadorRegistrador++);
    }

    /**
     * Aloca um novo rótulo (label) (ex: "L1", "L2").
     */
    private String alocarLabel() {
        return "L" + (contadorLabel++);
    }

    /**
     * Adiciona uma instrução à lista de código gerado.
     */
    private void emitir(String instrucao) {
        codigo.add(instrucao);
    }

    /**
     * Método principal que inicia a varredura da Árvore Sintática.
     */
    public void gerar(NoArvore no) {
        if (no == null) {
            return;
        }

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
            // Outros nós (Programa, DeclaracaoTipo) são apenas visitados
            default:
                // Visita os filhos recursivamente
                for (NoArvore filho : no.filhos) {
                    gerar(filho);
                }
                break;
        }
    }

    /**
     * Gera código para uma Atribuição.
     * Ex: b = b + 1
     */
    private void gerarAtribuicao(NoArvore noAtribuicao) {
        resetContadorRegistrador();

        // Filho 0 é o ID (ex: "b")
        NoArvore noVar = noAtribuicao.filhos.get(0);
        String nomeVar = noVar.valor;

        // Gera o código para a expressão (filhos a partir do índice 2)
        // e obtém o registrador que contém o resultado final.
        String regResultado = gerarExpressao(noAtribuicao, 2);

        // Armazena o resultado da expressão na variável 
        emitir("STORE " + nomeVar + ", " + regResultado);
    }

    /**
     * Gera código para um Condicional.
     * Ex: se (a > 10) entao ...
     */
    private void gerarCondicional(NoArvore noCondicional) {
        resetContadorRegistrador();
        
        // Estrutura: [se, Condicao, entao, Comando, ;, (senao, Comando)?]
        NoArvore noCondicao = noCondicional.filhos.get(1);
        NoArvore noComandoEntao = noCondicional.filhos.get(3);
        
        String labelFim = alocarLabel(); // Label para pular o bloco 'entao' 

        // 1. Gerar código para a Condição
        // Estrutura Condicao: [(, id, op, id/num, )]
        NoArvore termo1 = noCondicao.filhos.get(1);
        NoArvore op = noCondicao.filhos.get(2);
        NoArvore termo2 = noCondicao.filhos.get(3);

        String reg1 = carregarTermo(termo1); 
        String reg2 = carregarTermo(termo2); 
        
        String opMnem = traduzirOperadorLogico(op.valor);
        emitir(opMnem + " " + reg1 + ", " + reg2); 

        // 2. Gerar o Salto Condicional
        emitir("JMPFALSE " + reg1 + ", " + labelFim);
        
        // 3. Gerar código para o bloco 'entao'
        gerar(noComandoEntao);
        
        // 4. Emitir o Label de fim
        emitir("LABEL " + labelFim);
        
        // TODO: Adicionar lógica para 'senao' (exigiria um JMP incondicional
        // no final do bloco 'entao' e um novo label para o 'senao').
    }

    /**
     * Gera código para uma expressão (ex: b + 1)
     * Retorna o registrador que contém o resultado final.
     */
    private String gerarExpressao(NoArvore noPai, int indiceInicio) {
        // Pega o primeiro termo (ex: "b" de "b+1")
        NoArvore primeiroTermoNo = noPai.filhos.get(indiceInicio).filhos.get(0);
        String regAtual = carregarTermo(primeiroTermoNo);

        // Itera sobre os pares (operador, termo) restantes (ex: "+", "1")
        for (int i = indiceInicio + 1; i < noPai.filhos.size(); i += 2) {
            String op = noPai.filhos.get(i).valor; // "+"
            NoArvore proximoTermoNo = noPai.filhos.get(i + 1).filhos.get(0); // "1"

            // Otimização: Usar ADDI, SUBI, etc., se o segundo termo for um número
            if (isNumero(proximoTermoNo.valor)) {
                String opImediato = traduzirOperadorAritmeticoImediato(op);
                emitir(opImediato + " " + regAtual + ", " + proximoTermoNo.valor); // ex: ADDI R1, 1
            } else {
                // Se for outra variável, carrega e usa a operação padrão
                String regProximo = carregarTermo(proximoTermoNo);
                String opPadrao = traduzirOperadorAritmetico(op);
                emitir(opPadrao + " " + regAtual + ", " + regAtual + ", " + regProximo); // ex: ADD R1, R1, R2 [cite: 133]
            }
        }
        return regAtual; // O resultado final permanece no primeiro registrador
    }

    private void gerarIterativo(NoArvore noIterativo) {
        resetContadorRegistrador();

        String labelInicio = alocarLabel();
        String labelFim = alocarLabel();

        // Estrutura AST: [enquanto, Condicao, Comando]
        NoArvore noCondicao = noIterativo.filhos.get(1);
        NoArvore noComando = noIterativo.filhos.get(2);

        // 1. Label de início (para voltar e repetir o loop)
        emitir("LABEL " + labelInicio);

        // 2. Avaliação da condição
        // (Reutiliza lógica similar ao condicional, simplificado aqui para condição simples)
        NoArvore termo1 = noCondicao.filhos.get(0).filhos.get(0); // Dentro de CondicaoSimples
        NoArvore op = noCondicao.filhos.get(0).filhos.get(1);
        NoArvore termo2 = noCondicao.filhos.get(0).filhos.get(2);

        String reg1 = carregarTermo(termo1);
        String reg2 = carregarTermo(termo2);
        String opMnem = traduzirOperadorLogico(op.valor);

        emitir(opMnem + " " + reg1 + ", " + reg2);

        // 3. Se FALSO, pula para fora
        emitir("JMPFALSE " + reg1 + ", " + labelFim);

        // 4. Corpo do loop
        gerar(noComando);

        // 5. Salto incondicional para o início
        emitir("JMP " + labelInicio);

        // 6. Label de fim
        emitir("LABEL " + labelFim);
    }

    /**
     * Carrega um termo (ID ou Número) para um novo registrador.
     * Retorna o nome do registrador onde o valor foi carregado.
     */
    private String carregarTermo(NoArvore noTermo) {
        String reg = alocarRegistrador();
        if (isNumero(noTermo.valor)) {
            emitir("LOADI " + reg + ", " + noTermo.valor); // Carrega Imediato 
        } else {
            emitir("LOAD " + reg + ", " + noTermo.valor); // Carrega da Memória (variável) 
        }
        return reg;
    }

    // --- Métodos Utilitários ---

    private boolean isNumero(String s) {
        return Character.isDigit(s.charAt(0));
    }

    private String traduzirOperadorLogico(String op) {
        switch (op) {
            case ">":  return "CMPGT"; // Greater Than
            case "<":  return "CMPLT"; // Less Than
            case "==": return "CMPEQ"; // Equal
            case ">=": return "CMPGE"; // Greater Equal (Estendido)
            case "<=": return "CMPLE"; // Less Equal (Estendido)
            case "!=": return "CMPNE"; // Not Equal (Estendido)
            default:
                throw new RuntimeException("Operador lógico não suportado: " + op);
        }
    }

    private String traduzirOperadorAritmetico(String op) {
        switch (op) {
            case "+": return "ADD"; 
            case "-": return "SUB"; 
            case "*": return "MUL"; 
            case "/": return "DIV"; 
            default:
                throw new RuntimeException("Operador aritmético não suportado: " + op);
        }
    }
    
    private String traduzirOperadorAritmeticoImediato(String op) {
        switch (op) {
            case "+": return "ADDI"; 
            case "-": return "SUBI"; 
            // Multiplicação e Divisão Imediata não são listadas, 
            // então elas usariam a versão padrão (carregando em registrador).
            default:
                return traduzirOperadorAritmetico(op);
        }
    }
}