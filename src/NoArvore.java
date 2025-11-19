import java.util.ArrayList;
import java.util.List;

public class NoArvore {
    public String valor;
    public int linha;
    public List<NoArvore> filhos;

    public NoArvore(String valor, int linha) { 
        this.valor = valor;
        this.linha = linha; 
        this.filhos = new ArrayList<>();
    }

    public void adicionarFilho(NoArvore filho) {
        this.filhos.add(filho);
    }
}