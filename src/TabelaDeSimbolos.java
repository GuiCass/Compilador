import java.util.HashMap;
import java.util.Map;

/**
 * Estrutura de dados que armazena informações sobre os identificadores (variáveis) declarados.
 * Fundamental para a análise semântica.
 */
public class TabelaDeSimbolos {

    // Armazena par (Nome da Variável, Tipo da Variável)
    private Map<String, String> simbolos;

    public TabelaDeSimbolos() {
        this.simbolos = new HashMap<>();
    }

    /**
     * Registra uma nova variável. Lança erro se duplicada.
     */
    public void declarar(String nome, String tipo, int linha) {
        if (simbolos.containsKey(nome)) {
            throw new RuntimeException("Erro Semântico: Variável '" + nome + "' já declarada. Linha " + linha);
        }
        simbolos.put(nome, tipo);
    }

    /**
     * Verifica existência e retorna o tipo. Lança erro se não encontrada.
     */
    public String verificarDeclarada(String nome, int linha) {
        if (!simbolos.containsKey(nome)) {
            throw new RuntimeException("Erro Semântico: Variável '" + nome + "' não declarada. Linha " + linha);
        }
        return simbolos.get(nome);
    }

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