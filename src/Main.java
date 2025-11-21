import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;

/**
 * Classe principal que orquestra todo o processo de compilação.
 * Responsável por ler o arquivo de código e executar sequencialmente as fases:
 * 1. Léxica
 * 2. Sintática
 * 3. Semântica
 * 4. Geração de Código
 */
public class Main {
    public static void main(String[] args) throws IOException {

        // Leitura do arquivo de entrada contendo o código fonte
        String codigo = Files.readString(Path.of("código.txt"));

        try {
            // ---------------------------------------------------------
            // Fase 1: Análise Léxica
            // Transforma o texto bruto em uma sequência de tokens.
            // ---------------------------------------------------------
            System.out.println("Iniciando Fase 1: Léxica...");

            // Instância usada para passar ao sintático
            AnalisadorLexico lexico = new AnalisadorLexico(codigo);
            // Instância separada apenas para impressão/debug, para não consumir os tokens da principal
            AnalisadorLexico lexicoParaImpressao = new AnalisadorLexico(codigo);

            StringBuilder saidaLexica = new StringBuilder();
            saidaLexica.append("--- Fase 1: Análise Léxica (Lista de Tokens) ---\n");
            Token t;
            // Itera sobre todos os tokens até encontrar o fim do arquivo (EOF)
            while((t = lexicoParaImpressao.proximoToken()).tipo != TipoToken.EOF) {
                saidaLexica.append(t.toString()).append("\n");
            }
            saidaLexica.append("-------------------------------------------------\n");
            escreverArquivo("fase1_lexico.txt", saidaLexica.toString());
            System.out.println("Fase 1 concluída. Saída em fase1_lexico.txt");

            pausar(); // Pausa para visualização do fluxo

            // ---------------------------------------------------------
            // Fase 2: Análise Sintática
            // Verifica a estrutura gramatical e constrói a Árvore Sintática.
            // ---------------------------------------------------------
            System.out.println("Iniciando Fase 2: Sintática...");
            AnalisadorSintatico sintatico = new AnalisadorSintatico(lexico);

            // Inicia a análise a partir da regra inicial 'programa'
            NoArvore arvoreSintatica = sintatico.programa();

            StringBuilder saidaSintatica = new StringBuilder();
            saidaSintatica.append("--- Fase 2: Análise Sintática (Arvore) ---\n");
            // Gera a representação visual da árvore para o arquivo de saída
            construirStringArvore(arvoreSintatica, "", true, saidaSintatica);
            saidaSintatica.append("Análise sintática concluída com sucesso!\n");
            saidaSintatica.append("-------------------------------------------------\n");
            escreverArquivo("fase2_sintatico.txt", saidaSintatica.toString());
            System.out.println("Fase 2 concluída. Saída em fase2_sintatico.txt");

            pausar();

            // ---------------------------------------------------------
            // Fase 3: Análise Semântica
            // Verifica regras de contexto, como tipos de variáveis e declarações.
            // ---------------------------------------------------------
            System.out.println("Iniciando Fase 3: Semântica...");
            // Recupera a tabela preenchida durante a fase sintática
            TabelaDeSimbolos tabela = sintatico.getTabelaDeSimbolos();
            AnalisadorSemantico semantico = new AnalisadorSemantico(tabela);

            // Percorre a árvore sintática validando as regras semânticas
            semantico.analisar(arvoreSintatica);

            StringBuilder saidaSemantica = new StringBuilder();
            saidaSemantica.append("--- Fase 3: Análise Semântica (Tabela Símbolos) ---\n");
            saidaSemantica.append(tabela.toString());
            saidaSemantica.append("Análise semântica concluída com sucesso!\n");
            saidaSemantica.append("-------------------------------------------------\n");
            escreverArquivo("fase3_semantico.txt", saidaSemantica.toString());
            System.out.println("Fase 3 concluída. Saída em fase3_semantico.txt");

            pausar();

            // ---------------------------------------------------------
            // Fase 4: Geração de Código Intermediário
            // Converte a árvore sintática em instruções de baixo nível (TAC).
            // ---------------------------------------------------------
            System.out.println("Iniciando Fase 4: Geração de Código...");
            GeradorCodigoIntermediario gerador = new GeradorCodigoIntermediario();
            gerador.gerar(arvoreSintatica);

            StringBuilder saidaCodigo = new StringBuilder();
            saidaCodigo.append("--- Fase 4: Código Intermediário (TAC) ---\n");
            for (String instrucao : gerador.getCodigo()) {
                saidaCodigo.append(instrucao).append("\n");
            }
            saidaCodigo.append("Geração de código concluída com sucesso!\n");
            saidaCodigo.append("-------------------------------------------------\n");
            escreverArquivo("fase4_codigo.txt", saidaCodigo.toString());
            System.out.println("Fase 4 concluída. Saída em fase4_codigo.txt");

        } catch (RuntimeException e) {
            // Tratamento centralizado de erros de compilação (Léxico, Sintático, Semântico)
            System.err.println("\n--- ERRO ---");
            System.err.println("Erro detectado: " + e.getMessage());
            System.err.println("Verifique 'erro.txt' para detalhes.");

            try {
                StringBuilder erroBuilder = new StringBuilder();
                erroBuilder.append("--- ERRO DE COMPILAÇÃO ---\n");
                erroBuilder.append(e.getMessage()).append("\n\n");
                erroBuilder.append("--- Stack Trace ---\n");
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                erroBuilder.append(sw.toString());

                escreverArquivo("erro.txt", erroBuilder.toString());
            } catch (IOException ioEx) {
                System.err.println("Erro CRÍTICO: Não foi possível escrever o arquivo de erro.");
                ioEx.printStackTrace();
            }

        } catch (IOException e) {
            System.err.println("Erro fatal de I/O ao escrever arquivo de saída: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método auxiliar recursivo para formatar a impressão da Árvore Sintática.
     * @param no O nó atual sendo processado.
     * @param prefixo A string de indentação acumulada.
     * @param isUltimo Indica se é o último filho, alterando o caractere de desenho (└ vs ├).
     * @param sb StringBuilder onde a árvore será montada.
     */
    public static void construirStringArvore(NoArvore no, String prefixo, boolean isUltimo, StringBuilder sb) {
        if (no == null) return;

        sb.append(prefixo)
                .append(isUltimo ? "└── " : "├── ")
                .append(no.valor)
                .append(" (L")
                .append(no.linha)
                .append(")\n");

        String prefixoFilho = prefixo + (isUltimo ? "    " : "│   ");

        for (int i = 0; i < no.filhos.size(); i++) {
            NoArvore filho = no.filhos.get(i);
            boolean ultimoFilho = (i == no.filhos.size() - 1);
            construirStringArvore(filho, prefixoFilho, ultimoFilho, sb);
        }
    }

    // Escreve o conteúdo em um arquivo físico no disco
    private static void escreverArquivo(String nomeArquivo, String conteudo) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(nomeArquivo))) {
            out.print(conteudo);
        }
    }

    // Realiza uma pausa na execução para facilitar o acompanhamento visual
    private static void pausar() {
        System.out.println("...pausando por 5 segundos...\n");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Pausa interrompida.");
        }
    }
}