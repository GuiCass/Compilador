import java.util.ArrayList;
import java.util.List;

/**
 * Nó genérico para construção da Árvore Sintática (Árvore N-ária).
 */
public class NoArvore {
    public String valor;           // Nome da regra ou valor do token
    public int linha;              // Origem no código fonte
    public List<NoArvore> filhos;  // Lista de sub-nós

    public NoArvore(String valor, int linha) {
        this.valor = valor;
        this.linha = linha;
        this.filhos = new ArrayList<>();
    }

    public void adicionarFilho(NoArvore filho) {
        this.filhos.add(filho);
    }
}