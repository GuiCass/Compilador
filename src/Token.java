public class Token {
	
    public final TipoToken tipo;
    public final String lexema;
    public final int linha;

    public Token(TipoToken tipo, String lexema, int linha) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
    }

    @Override
    public String toString() {
        return "Token{" +
               "tipo=" + tipo +
               ", lexema='" + lexema + '\'' +
               ", linha=" + linha +
               '}';
    }
}