package minipascal.lexico.io;

import minipascal.lexico.model.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Grava a lista de tokens no arquivo de saída, um par <lexema, token> por
 * linha (usa o Token.toString(), já no formato exigido), sempre em UTF-8.
 */
public class EscritorArquivoSaida {

    /** "prog.txt" -> "prog_saida.txt"; sem extensão, só acrescenta o sufixo. */
    public static String nomeDeSaidaPara(String nomeArquivo) {
        int ponto = nomeArquivo.lastIndexOf('.');
        String base = ponto > 0 ? nomeArquivo.substring(0, ponto) : nomeArquivo;
        return base + "_saida.txt";
    }

    public static void escrever(String caminho, List<Token> tokens) throws IOException {
        StringBuilder conteudo = new StringBuilder();
        for (Token token : tokens) {
            conteudo.append(token.toString()).append('\n');
        }
        Files.write(Paths.get(caminho), conteudo.toString().getBytes(StandardCharsets.UTF_8));
    }
}