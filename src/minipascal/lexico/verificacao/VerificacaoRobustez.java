package minipascal.lexico.verificacao;

import minipascal.lexico.Main;
import minipascal.lexico.core.AnalisadorLexico;
import minipascal.lexico.io.EscritorArquivoSaida;
import minipascal.lexico.io.LeitorArquivoFonte;
import minipascal.lexico.model.Token;
import minipascal.lexico.model.TipoToken;
import minipascal.lexico.tabela.TabelaPalavrasReservadas;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Entradas e ambientes fora do comum: Unicode, quebras de linha antigas,
 * codificações diferentes de UTF-8, locale do sistema e caminhos do CLI.
 */
public class VerificacaoRobustez {

    private static int total = 0;
    private static int falhas = 0;

    public static void main(String[] args) throws IOException {
        checarLetrasLatinasEDigitosAscii();
        checarEmojiViraUmErroSo();
        checarCharComEmoji();
        checarCrSolto();
        checarStringNaoFechadaNaoLevaCr();
        checarEspacosUnicode();
        checarLocaleTurco();
        checarLeituraAnsi();
        checarLeituraUtf16();
        checarNomeDeSaida();
        checarCliComCaminhoRelativoEPontoNaPasta();
        checarCliRecusaSobrescreverEntrada();
        checarVolumeGrande();

        System.out.println();
        System.out.println(total + " verificações de robustez, " + falhas + " falha(s).");
        if (falhas > 0) {
            System.exit(1);
        }
    }

    private static void checarLetrasLatinasEDigitosAscii() {
        List<Token> t = tokenizar("variável ação Ñandú");
        checar("identificadores com acento são aceitos (exemplo 'variável' do enunciado)",
                t.size() == 3 && t.get(0).getLexema().equals("variável")
                        && t.get(1).getLexema().equals("ação") && t.get(2).getLexema().equals("Ñandú")
                        && t.get(0).getTipo() == TipoToken.IDENTIFICADOR);

        t = tokenizar("\u03c0 \u65e5\u672c");
        checar("letras gregas e CJK são erro léxico, não identificador",
                t.size() == 3 && todosErro(t));

        t = tokenizar("\u0663\u0664 \uff11\uff12");
        checar("dígitos árabes e de largura total são erro léxico, não número",
                t.size() == 4 && todosErro(t));

        t = tokenizar("\"ação\" // coment\u00e1rio");
        checar("acentos continuam válidos dentro de string",
                t.get(0).getTipo() == TipoToken.CONSTANTE_STRING && t.get(0).getLexema().equals("\"ação\""));
    }

    private static void checarEmojiViraUmErroSo() {
        String emoji = "😀";
        List<Token> t = tokenizar("x " + emoji + " y");
        checar("emoji gera um único token de erro com o par de surrogates inteiro",
                t.size() == 3 && t.get(1).getTipo() == TipoToken.ERRO_LEXICO && t.get(1).getLexema().equals(emoji));
    }

    private static void checarCharComEmoji() {
        String emoji = "😀";
        List<Token> t = tokenizar("'" + emoji + "'");
        checar("char com um emoji é um único Constante char",
                t.size() == 1 && t.get(0).getTipo() == TipoToken.CONSTANTE_CHAR);
    }

    private static void checarCrSolto() {
        List<Token> t = tokenizar("a\rb\rc");
        checar("'\\r' sozinho conta como quebra de linha (linhas 1, 2, 3)",
                t.size() == 3 && t.get(0).getLinha() == 1 && t.get(1).getLinha() == 2 && t.get(2).getLinha() == 3);

        t = tokenizar("a\r\nb\r\n/* x\r\ny */\r\nc");
        checar("CRLF conta uma linha só, inclusive dentro de comentário (a=1, b=2, c=5)",
                t.size() == 3 && t.get(0).getLinha() == 1 && t.get(1).getLinha() == 2 && t.get(2).getLinha() == 5);
    }

    private static void checarStringNaoFechadaNaoLevaCr() {
        List<Token> t = tokenizar("\"abc\r\nx");
        checar("string não fechada em arquivo CRLF não leva o '\\r' no lexema",
                t.size() == 2 && t.get(0).getLexema().equals("\"abc") && t.get(1).getLinha() == 2);

        t = tokenizar("'a\r\nx");
        checar("char não fechado em arquivo CRLF não leva o '\\r' no lexema",
                t.size() == 2 && t.get(0).getLexema().equals("'a"));
    }

    private static void checarEspacosUnicode() {
        List<Token> t = tokenizar("a b");
        checar("espaço não separável (NBSP) é reportado como erro, não ignorado",
                t.size() == 3 && t.get(1).getTipo() == TipoToken.ERRO_LEXICO);

        t = tokenizar("a\fb\u000bc\td");
        checar("form feed, tab vertical e tab separam tokens",
                t.size() == 4 && t.get(3).getLexema().equals("d"));
    }

    private static void checarLocaleTurco() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            TabelaPalavrasReservadas tabela = new TabelaPalavrasReservadas();
            checar("locale turco: 'INTEGER' continua palavra reservada",
                    tabela.buscar("INTEGER") == TipoToken.PALAVRA_RESERVADA);
            checar("locale turco: 'DIV' continua palavra reservada",
                    tabela.buscar("DIV") == TipoToken.PALAVRA_RESERVADA);
            checar("locale turco: 'Title' continua identificador",
                    tabela.buscar("Title") == null);
        } finally {
            Locale.setDefault(original);
        }
    }

    private static void checarLeituraAnsi() throws IOException {
        File f = temporario("ansi", ".txt");
        Files.write(f.toPath(), new byte[]{'"', 'a', (byte) 0xE7, (byte) 0xE3, 'o', '"'});
        String lido = LeitorArquivoFonte.ler(f.getAbsolutePath());
        checar("arquivo ANSI (ISO-8859-1) é lido sem perder a acentuação", lido.equals("\"ação\""));
    }

    private static void checarLeituraUtf16() throws IOException {
        File f = temporario("utf16", ".txt");
        Files.write(f.toPath(), "program x;".getBytes(StandardCharsets.UTF_16));
        String lido = LeitorArquivoFonte.ler(f.getAbsolutePath());
        checar("arquivo UTF-16 com BOM é lido corretamente", lido.equals("program x;"));
    }

    private static void checarNomeDeSaida() {
        checar("'prog.txt' -> 'prog_saida.txt'",
                EscritorArquivoSaida.nomeDeSaidaPara("prog.txt").equals("prog_saida.txt"));
        checar("sem extensão -> acrescenta o sufixo",
                EscritorArquivoSaida.nomeDeSaidaPara("prog").equals("prog_saida.txt"));
        checar("'a.b.txt' -> 'a.b_saida.txt'",
                EscritorArquivoSaida.nomeDeSaidaPara("a.b.txt").equals("a.b_saida.txt"));
        checar("'.oculto' não é tratado como extensão",
                EscritorArquivoSaida.nomeDeSaidaPara(".oculto").equals(".oculto_saida.txt"));
    }

    private static void checarCliComCaminhoRelativoEPontoNaPasta() throws IOException {
        File raiz = Files.createTempDirectory("tokenflow").toFile();
        File pasta = new File(raiz, "dir.v2");
        pasta.mkdirs();
        File entrada = new File(pasta, "prog");
        Files.write(entrada.toPath(), "program x;".getBytes(StandardCharsets.UTF_8));

        Main.main(new String[]{entrada.getAbsolutePath()});
        checar("CLI: entrada 'dir.v2/prog' (ponto na pasta, sem extensão) gera 'dir.v2/prog_saida.txt'",
                new File(pasta, "prog_saida.txt").isFile() && !new File(raiz, "dir_saida.txt").exists());

        File sub = new File(pasta, "sub");
        sub.mkdirs();
        Main.main(new String[]{new File(sub, "..").getPath() + File.separator + "prog"});
        checar("CLI: caminho com '..' não cria arquivo '._saida.txt'",
                !new File(".", "._saida.txt").exists());
    }

    private static void checarCliRecusaSobrescreverEntrada() throws IOException {
        File f = temporario("mesmo", ".txt");
        Files.write(f.toPath(), "program x;".getBytes(StandardCharsets.UTF_8));
        Main.main(new String[]{f.getAbsolutePath(), f.getAbsolutePath()});
        String depois = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        checar("CLI: saída igual à entrada é recusada e o arquivo original fica intacto",
                depois.equals("program x;"));
    }

    private static void checarVolumeGrande() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200000; i++) {
            sb.append("x := a + 1;\n");
        }
        long inicio = System.nanoTime();
        List<Token> t = tokenizar(sb.toString());
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        checar("200 mil linhas (1,2 milhão de tokens) em tempo linear (" + ms + " ms)",
                t.size() == 1_200_000 && t.get(t.size() - 1).getLinha() == 200000 && ms < 10_000);

        char[] enorme = new char[1_000_000];
        java.util.Arrays.fill(enorme, 'z');
        t = tokenizar(new String(enorme));
        checar("identificador de 1 milhão de caracteres vira 1 token de 63",
                t.size() == 1 && t.get(0).getLexema().length() == 63);
    }

    // ---------- utilitários ----------

    private static File temporario(String prefixo, String sufixo) throws IOException {
        File f = File.createTempFile(prefixo, sufixo);
        f.deleteOnExit();
        return f;
    }

    private static List<Token> tokenizar(String fonte) {
        AnalisadorLexico lexer = new AnalisadorLexico(fonte, new TabelaPalavrasReservadas());
        List<Token> tokens = new ArrayList<>();
        Token t;
        while ((t = lexer.proximoToken()) != null) {
            tokens.add(t);
        }
        return tokens;
    }

    private static boolean todosErro(List<Token> tokens) {
        for (Token t : tokens) {
            if (t.getTipo() != TipoToken.ERRO_LEXICO) {
                return false;
            }
        }
        return true;
    }

    private static void checar(String descricao, boolean condicao) {
        total++;
        if (condicao) {
            System.out.println("[OK]    " + descricao);
        } else {
            falhas++;
            System.out.println("[FALHA] " + descricao);
        }
    }
}