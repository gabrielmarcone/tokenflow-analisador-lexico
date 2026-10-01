package minipascal.lexico;

import minipascal.lexico.core.AnalisadorLexico;
import minipascal.lexico.io.EscritorArquivoSaida;
import minipascal.lexico.io.LeitorArquivoFonte;
import minipascal.lexico.model.Token;
import minipascal.lexico.tabela.TabelaPalavrasReservadas;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    // Modo linha de comando: le o arquivo, chama o analisador ate ele devolver
    // null e grava um par lexema/token por linha. Erros lexicos entram na saida
    // como tokens; so falha de leitura ou gravacao interrompe a execucao.
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java -cp TokenFlow.jar minipascal.lexico.Main <entrada.txt> [saida.txt]");
            return;
        }

        String caminhoEntrada = args[0];
        String caminhoSaida = args.length >= 2 ? args[1] : gerarNomeSaida(caminhoEntrada);

        // Evita apagar o proprio arquivo de entrada ao gravar a saida.
        if (new File(caminhoSaida).getAbsoluteFile().equals(new File(caminhoEntrada).getAbsoluteFile())) {
            System.out.println("O arquivo de saida nao pode ser o mesmo da entrada.");
            return;
        }

        try {
            String fonte = LeitorArquivoFonte.ler(caminhoEntrada);
            TabelaPalavrasReservadas tabela = new TabelaPalavrasReservadas();
            AnalisadorLexico lexer = new AnalisadorLexico(fonte, tabela);

            List<Token> tokens = new ArrayList<>();
            Token token;
            while ((token = lexer.proximoToken()) != null) {
                tokens.add(token);
            }

            EscritorArquivoSaida.escrever(caminhoSaida, tokens);
            System.out.println(tokens.size() + " tokens reconhecidos. Saida gravada em " + caminhoSaida);
        } catch (NoSuchFileException e) {
            System.out.println("Arquivo nao encontrado: " + e.getFile());
        } catch (IOException e) {
            System.out.println("Erro ao processar o arquivo: " + e.getMessage());
        }
    }

    private static String gerarNomeSaida(String caminhoEntrada) {
        File entrada = new File(caminhoEntrada);
        return new File(entrada.getParentFile(), EscritorArquivoSaida.nomeDeSaidaPara(entrada.getName())).getPath();
    }
}