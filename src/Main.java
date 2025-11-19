import java.io.FileWriter; 
import java.io.IOException; 
import java.io.PrintWriter; 
import java.io.StringWriter;
import java.nio.file.*;

public class Main {
    public static void main(String[] args) throws IOException {

        String codigo = Files.readString(Path.of("código.txt"));

        try {
            // --- Fase 1: Análise Léxica ---
            System.out.println("Iniciando Fase 1: Léxica...");
            AnalisadorLexico lexico = new AnalisadorLexico(codigo); 
            AnalisadorLexico lexicoParaImpressao = new AnalisadorLexico(codigo);
            
            StringBuilder saidaLexica = new StringBuilder();
            saidaLexica.append("--- Fase 1: Análise Léxica (Lista de Tokens) ---\n");
            Token t;
            while((t = lexicoParaImpressao.proximoToken()).tipo != TipoToken.EOF) {
                saidaLexica.append(t.toString()).append("\n");
            }
            saidaLexica.append("-------------------------------------------------\n");
            escreverArquivo("fase1_lexico.txt", saidaLexica.toString());
            System.out.println("Fase 1 concluída. Saída em fase1_lexico.txt");

            pausar();

            
            // --- Fase 2: Análise Sintática ---
            System.out.println("Iniciando Fase 2: Sintática...");
            AnalisadorSintatico sintatico = new AnalisadorSintatico(lexico);
            NoArvore arvoreSintatica = sintatico.programa();
            
            StringBuilder saidaSintatica = new StringBuilder();
            saidaSintatica.append("--- Fase 2: Análise Sintática (Arvore) ---\n");
            construirStringArvore(arvoreSintatica, "", true, saidaSintatica);
            saidaSintatica.append("Análise sintática concluída com sucesso!\n");
            saidaSintatica.append("-------------------------------------------------\n");
            escreverArquivo("fase2_sintatico.txt", saidaSintatica.toString());
            System.out.println("Fase 2 concluída. Saída em fase2_sintatico.txt");

            pausar();
            
            
            // --- Fase 3: Análise Semântica ---
            System.out.println("Iniciando Fase 3: Semântica...");
            TabelaDeSimbolos tabela = sintatico.getTabelaDeSimbolos();
            AnalisadorSemantico semantico = new AnalisadorSemantico(tabela);
            semantico.analisar(arvoreSintatica);
            
            StringBuilder saidaSemantica = new StringBuilder();
            saidaSemantica.append("--- Fase 3: Análise Semântica (Tabela Símbolos) ---\n");
            saidaSemantica.append(tabela.toString());
            saidaSemantica.append("Análise semântica concluída com sucesso!\n");
            saidaSemantica.append("-------------------------------------------------\n");
            escreverArquivo("fase3_semantico.txt", saidaSemantica.toString());
            System.out.println("Fase 3 concluída. Saída em fase3_semantico.txt");

            pausar();
            
            // --- Fase 4: Geração de Código Intermediário ---
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
            // Captura erros léxicos, sintáticos ou semânticos
            System.err.println("\n--- ERRO ---");
            System.err.println("Erro detectado: " + e.getMessage());
            System.err.println("Verifique 'erro.txt' para detalhes.");
            
            try {
                StringBuilder erroBuilder = new StringBuilder();
                erroBuilder.append("--- ERRO DE COMPILAÇÃO ---\n");
                erroBuilder.append(e.getMessage()).append("\n\n");
                erroBuilder.append("--- Stack Trace ---\n");
                // Converte a stack trace em String
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                erroBuilder.append(sw.toString());

                escreverArquivo("erro.txt", erroBuilder.toString());
            } catch (IOException ioEx) {
                System.err.println("Erro CRÍTICO: Não foi possível escrever o arquivo de erro.");
                ioEx.printStackTrace();
            }

        } catch (IOException e) {
            // Captura erros de escrita de arquivo
             System.err.println("Erro fatal de I/O ao escrever arquivo de saída: " + e.getMessage());
             e.printStackTrace();
        }
    }
    
    public static void construirStringArvore(NoArvore no, String prefixo, boolean isUltimo, StringBuilder sb) {
        if (no == null) {
            return;
        }
        
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

    private static void escreverArquivo(String nomeArquivo, String conteudo) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(nomeArquivo))) {
            out.print(conteudo);
        }
    }

    private static void pausar() {
        System.out.println("...pausando por 5 segundos...\n");
        try {
            Thread.sleep(5000); // 5000 milissegundos = 5 segundos
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Pausa interrompida.");
        }
    }
}