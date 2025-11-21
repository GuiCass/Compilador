// Token.java
/**
 * Representa a unidade mínima de informação extraída pelo analisador léxico.
 */
public class Token {
    public final TipoToken tipo; // Categoria do token (ex: NUMERO, IDENTIFICADOR, SE)
    public final String lexema;  // O texto exato encontrado no código fonte
    public final int linha;      // Linha onde foi encontrado (para mensagens de erro)

    public Token(TipoToken tipo, String lexema, int linha) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
    }

    // ... (toString omitido)
}