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
        String labelSenao = alocarLabel(); // Usado se houver senao

        // Avalia a condição e devolve o registrador com o resultado booleano (ou faz o salto)
        // Nota: Aqui assumimos condição simples para manter compatibilidade com TAC básico
        processarCondicaoSimples(noCondicao, labelFim);

        // Bloco Entao
        gerar(noComandoEntao);

        // Verifica se tem Senao
        if (noCondicional.filhos.size() > 5 && noCondicional.filhos.get(4).valor.equals("senao")) {
            String labelFinalReal = alocarLabel();
            emitir("JMP " + labelFinalReal); // Pula o senao ao terminar o entao

            emitir("LABEL " + labelFim); // O salto falso cai aqui (inicio do senao)
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

        // Avalia condição e pula para labelFim se for falso
        processarCondicaoSimples(noCondicao, labelFim);

        gerar(noComando);
        emitir("JMP " + labelInicio);
        emitir("LABEL " + labelFim);
    }

    // Método auxiliar para lidar com a nova estrutura da árvore de condição
    private void processarCondicaoSimples(NoArvore noCondicao, String labelDestinoSeFalso) {
        // A AST agora é: Condicao -> CondicaoSimples -> [(, id, op, id/num, )]
        // Vamos pegar o primeiro filho que deve ser CondicaoSimples
        if (noCondicao.filhos.get(0).valor.equals("CondicaoSimples")) {
            NoArvore noSimples = noCondicao.filhos.get(0);

            // Índices no CondicaoSimples: 0='(', 1=ID, 2=OP, 3=VALOR, 4=')'
            NoArvore termo1 = noSimples.filhos.get(1);
            NoArvore op = noSimples.filhos.get(2);
            NoArvore termo2 = noSimples.filhos.get(3);

            String reg1 = carregarTermo(termo1);
            String reg2 = carregarTermo(termo2);
            String opMnem = traduzirOperadorLogico(op.valor);

            emitir(opMnem + " " + reg1 + ", " + reg2);
            emitir("JMPFALSE " + reg1 + ", " + labelDestinoSeFalso);
        } else {
            // Caso para NOT ou complexas (Simplificação: trata como erro ou ignora)
            // Para suportar NOT/E/OR, seria necessário uma lógica recursiva de labels.
            throw new RuntimeException("Gerador de Código: Condições complexas (NOT/E/OR) ainda não implementadas no backend.");
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
            case "-": return "SUB";
            case "*": return "MUL";
            case "/": return "DIV";
            case "RESTO": return "MOD"; // Implementado RESTO
            default: throw new RuntimeException("Op aritmético inválido: " + op);
        }
    }

    private String traduzirOperadorAritmeticoImediato(String op) {
        switch (op) {
            case "+": return "ADDI";
            case "-": return "SUBI";
            // Multiplicação, Divisão e Resto não costumam ter imediatos em TAC simples
            default: return traduzirOperadorAritmetico(op);
        }
    }
}