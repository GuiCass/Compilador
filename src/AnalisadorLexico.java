import java.util.HashMap;
import java.util.Map;

/**
 * Responsável por ler o código fonte caractere a caractere e agrupá-los em Tokens.
 * Implementa a lógica de autômato finito para reconhecimento de padrões.
 */
public class AnalisadorLexico {
    private final String codigoFonte; // O código fonte completo carregado em memória
    private int posicaoAtual = 0;     // Ponteiro para o caractere sendo lido
    private int linhaAtual = 1;       // Contador de linhas para reporte de erros

    // Mapa estático contendo as palavras reservadas da linguagem para consulta rápida
    private static final Map<String, TipoToken> palavrasReservadas;

    static {
        palavrasReservadas = new HashMap<>();
        palavrasReservadas.put("inteiro", TipoToken.TIPO_INTEIRO);
        palavrasReservadas.put("real", TipoToken.TIPO_REAL);
        palavrasReservadas.put("caracter", TipoToken.TIPO_CARACTER);
        palavrasReservadas.put("se", TipoToken.SE);
        palavrasReservadas.put("entao", TipoToken.ENTAO);
        palavrasReservadas.put("senao", TipoToken.SENAO);
        palavrasReservadas.put("enquanto", TipoToken.ENQUANTO);
        palavrasReservadas.put("E", TipoToken.OP_BOOLEANO_E);
        palavrasReservadas.put("OR", TipoToken.OP_BOOLEANO_OR);
        palavrasReservadas.put("NOT", TipoToken.OP_BOOLEANO_NOT);
        palavrasReservadas.put("RESTO", TipoToken.OP_RESTO);
    }

    public AnalisadorLexico(String codigoFonte) {
        this.codigoFonte = codigoFonte;
    }

    /**
     * Obtém o próximo token válido do código fonte.
     * @return Objeto Token contendo tipo, lexema e linha.
     * @throws RuntimeException em caso de caracteres inválidos ou identificadores muito longos.
     */
    public Token proximoToken() {
        // Verifica se chegamos ao final do arquivo
        if (posicaoAtual >= codigoFonte.length()) {
            return new Token(TipoToken.EOF, "", linhaAtual);
        }

        char atual = codigoFonte.charAt(posicaoAtual);

        // Consome e ignora espaços em branco, tabulações e quebras de linha
        while (Character.isWhitespace(atual)) {
            if (atual == '\n') {
                linhaAtual++;
            }
            posicaoAtual++;
            if (posicaoAtual >= codigoFonte.length()) {
                return new Token(TipoToken.EOF, "", linhaAtual);
            }
            atual = codigoFonte.charAt(posicaoAtual);
        }

        // Se for dígito ou ponto, inicia a extração de um número
        if (Character.isDigit(atual) || atual == '.') {
            return extrairNumero();
        }

        // Se for letra, inicia a extração de identificador ou palavra reservada
        if (Character.isLetter(atual)) {
            return extrairIdentificador();
        }

        // Identificação de símbolos e operadores simples ou compostos
        switch (atual) {
            case '$':
                // Verifica se é o fim do programa '$.'
                if (posicaoAtual + 1 < codigoFonte.length() && codigoFonte.charAt(posicaoAtual + 1) == '.') {
                    posicaoAtual += 2;
                    return new Token(TipoToken.FIM_PROGRAMA, "$.", linhaAtual);
                }
                posicaoAtual++;
                return new Token(TipoToken.INICIO_PROGRAMA, "$", linhaAtual);
            case ';':
                posicaoAtual++;
                return new Token(TipoToken.PONTO_E_VIRGULA, ";", linhaAtual);
            case ',':
                posicaoAtual++;
                return new Token(TipoToken.VIRGULA, ",", linhaAtual);
            case '(':
                posicaoAtual++;
                return new Token(TipoToken.ABRE_PARENTESES, "(", linhaAtual);
            case ')':
                posicaoAtual++;
                return new Token(TipoToken.FECHA_PARENTESES, ")", linhaAtual);
            case '+':
                posicaoAtual++;
                return new Token(TipoToken.OP_SOMA, "+", linhaAtual);
            case '>':
                posicaoAtual++;
                if (posicaoAtual < codigoFonte.length() && codigoFonte.charAt(posicaoAtual) == '=') {
                    posicaoAtual++;
                    return new Token(TipoToken.OP_LOGICO, ">=", linhaAtual);
                }
                return new Token(TipoToken.OP_LOGICO, ">", linhaAtual);
            case '<':
                posicaoAtual++;
                if (posicaoAtual < codigoFonte.length() && codigoFonte.charAt(posicaoAtual) == '=') {
                    posicaoAtual++;
                    return new Token(TipoToken.OP_LOGICO, "<=", linhaAtual);
                }
                return new Token(TipoToken.OP_LOGICO, "<", linhaAtual);
            case '=':
                posicaoAtual++;
                if (posicaoAtual < codigoFonte.length() && codigoFonte.charAt(posicaoAtual) == '=') {
                    posicaoAtual++;
                    return new Token(TipoToken.OP_LOGICO, "==", linhaAtual);
                }
                return new Token(TipoToken.OP_ATRIBUICAO, "=", linhaAtual);
            case '!':
                posicaoAtual++;
                if (posicaoAtual < codigoFonte.length() && codigoFonte.charAt(posicaoAtual) == '=') {
                    posicaoAtual++;
                    return new Token(TipoToken.OP_LOGICO, "!=", linhaAtual);
                }
                throw new RuntimeException("Erro Léxico: Caractere inesperado '!' na linha " + linhaAtual);
            case '*':
                posicaoAtual++;
                return new Token(TipoToken.OP_MULT, "*", linhaAtual);
            case '/':
                posicaoAtual++;
                return new Token(TipoToken.OP_DIV, "/", linhaAtual);
        }
        throw new RuntimeException("Erro Léxico: Caractere inesperado '" + atual + "' na linha " + linhaAtual);
    }

    /**
     * Extrai uma sequência alfanumérica e verifica se é uma Palavra Reservada ou Identificador.
     * Aplica a regra de limite máximo de 10 caracteres.
     */
    private Token extrairIdentificador() {
        int inicio = posicaoAtual;
        while (posicaoAtual < codigoFonte.length() &&
                (Character.isLetterOrDigit(codigoFonte.charAt(posicaoAtual)))) {
            posicaoAtual++;
        }
        String lexema = codigoFonte.substring(inicio, posicaoAtual);

        // Validação da Premissa: Limite de caracteres
        if (lexema.length() > 10) {
            throw new RuntimeException("Erro Léxico: Identificador '" + lexema + "' excede o limite de 10 caracteres na linha " + linhaAtual);
        }

        // Verifica se o lexema extraído existe no mapa de palavras reservadas
        TipoToken tipo = palavrasReservadas.getOrDefault(lexema, TipoToken.IDENTIFICADOR);
        return new Token(tipo, lexema, linhaAtual);
    }

    /**
     * Extrai uma sequência numérica (inteiro ou real).
     */
    private Token extrairNumero() {
        int inicio = posicaoAtual;
        while (posicaoAtual < codigoFonte.length() &&
                (Character.isDigit(codigoFonte.charAt(posicaoAtual)) || codigoFonte.charAt(posicaoAtual) == '.')) {
            posicaoAtual++;
        }
        String lexema = codigoFonte.substring(inicio, posicaoAtual);
        return new Token(TipoToken.NUMERO, lexema, linhaAtual);
    }
}