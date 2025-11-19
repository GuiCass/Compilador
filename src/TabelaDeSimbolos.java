import java.util.HashMap;
import java.util.Map;

public class TabelaDeSimbolos {

    // Usamos um HashMap para armazenar a variável (nome) e seu tipo (string)
    private Map<String, String> simbolos;

    public TabelaDeSimbolos() {
        this.simbolos = new HashMap<>();
    }

    /**
     * Declara uma nova variável na tabela.
     * Dispara um erro semântico se a variável já foi declarada. [cite: 58]
     */
    public void declarar(String nome, String tipo, int linha) {
        if (simbolos.containsKey(nome)) {
            throw new RuntimeException("Erro Semântico: Variável '" + nome + "' já declarada. Linha " + linha);
        }
        simbolos.put(nome, tipo);
    }

    /**
     * Verifica se uma variável foi declarada e retorna seu tipo.
     * Dispara um erro semântico se não foi declarada. 
     */
    public String verificarDeclarada(String nome, int linha) {
        if (!simbolos.containsKey(nome)) {
            throw new RuntimeException("Erro Semântico: Variável '" + nome + "' não declarada. Linha " + linha);
        }
        return simbolos.get(nome);
    }
    
    /**
     * Retorna o conteúdo da tabela para impressão.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Tabela de Símbolos ---\n");
        for (Map.Entry<String, String> entry : simbolos.entrySet()) {
            sb.append(String.format("ID: %-10s | Categoria: variável %s\n", entry.getKey(), entry.getValue()));
        }
        sb.append("----------------------------\n");
        return sb.toString();
    }
}