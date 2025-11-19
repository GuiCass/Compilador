import java.util.HashMap;
import java.util.Map;

public class AnalisadorLexico {
    private final String codigoFonte;
    private int posicaoAtual = 0;
    private int linhaAtual = 1;
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
        // ... outras palavras reservadas como 'RESTO'
    }

    public AnalisadorLexico(String codigoFonte) {
        this.codigoFonte = codigoFonte;
    }

    public Token proximoToken() {
        if (posicaoAtual >= codigoFonte.length()) {
            return new Token(TipoToken.EOF, "", linhaAtual);
        }

        char atual = codigoFonte.charAt(posicaoAtual);

        // Ignorar espaços em branco, tabulações e novas linhas
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

        // Reconhecer números
        if (Character.isDigit(atual) || atual == '.') {
            return extrairNumero();
        }

        // Reconhecer identificadores e palavras reservadas
        if (Character.isLetter(atual)) {
            return extrairIdentificador();
        }

        // Reconhecer símbolos e operadores
        switch (atual) {
            case '$':
                // Checa por '$.'
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
                // Se encontrou apenas '!', lança erro (pois não existe operador '!' sozinho na MLP)
                throw new RuntimeException("Erro Léxico: Caractere inesperado '!' na linha " + linhaAtual);
        }
        throw new RuntimeException("Erro Léxico: Caractere inesperado '" + atual + "' na linha " + linhaAtual);
    }
    
    private Token extrairIdentificador() {
        int inicio = posicaoAtual;
        while (posicaoAtual < codigoFonte.length() && 
               (Character.isLetterOrDigit(codigoFonte.charAt(posicaoAtual)))) {
            posicaoAtual++;
        }
        String lexema = codigoFonte.substring(inicio, posicaoAtual);
        
        // Premissa 1: Limite de 10 caracteres para identificador
        if (lexema.length() > 10) {
            throw new RuntimeException("Erro Léxico: Identificador '" + lexema + "' excede o limite de 10 caracteres na linha " + linhaAtual);
        }

        TipoToken tipo = palavrasReservadas.getOrDefault(lexema, TipoToken.IDENTIFICADOR);
        return new Token(tipo, lexema, linhaAtual);
    }

    private Token extrairNumero() {
        int inicio = posicaoAtual;
        while (posicaoAtual < codigoFonte.length() && 
               (Character.isDigit(codigoFonte.charAt(posicaoAtual)) || codigoFonte.charAt(posicaoAtual) == '.')) {
            // Validação mais robusta seria necessária para evitar "1.2.3"
            posicaoAtual++;
        }
        String lexema = codigoFonte.substring(inicio, posicaoAtual);
        return new Token(TipoToken.NUMERO, lexema, linhaAtual);
    }
}