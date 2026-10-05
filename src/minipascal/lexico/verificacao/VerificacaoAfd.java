package minipascal.lexico.verificacao;

import minipascal.lexico.core.AnalisadorLexico;
import minipascal.lexico.model.TipoToken;
import minipascal.lexico.model.Token;
import minipascal.lexico.tabela.TabelaPalavrasReservadas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Compara o AnalisadorLexico com o AFD descrito em docs/AFD_Analisador_Lexico.md.
 * O AFD esta escrito aqui como tabela de transicoes (estados q0..q37) e nao
 * compartilha codigo com o analisador: o modelo avanca ate travar e volta ao
 * ultimo estado final visitado. Cada entrada passa pelos dois e as listas de
 * lexemas e classes devem ser iguais.
 */
public class VerificacaoAfd {

    private static final TabelaPalavrasReservadas TABELA = new TabelaPalavrasReservadas();

    // Classes de caractere usadas nas transicoes. Qualquer outro rotulo de
    // uma letra e o proprio caractere.
    private static final String WS = "WS";
    private static final String NL = "NL";
    private static final String LETRA = "LETRA";
    private static final String DIGITO = "DIGITO";
    private static final String EOF = "EOF";
    private static final String OUTRO = "OUTRO";

    private static final int FIM_DE_ENTRADA = -1;
    private static final int TAMANHO_MAXIMO_IDENTIFICADOR = 63;

    private static class Regra {
        final String simbolo;
        final String destino;
        final boolean consome;

        Regra(String simbolo, String destino, boolean consome) {
            this.simbolo = simbolo;
            this.destino = destino;
            this.consome = consome;
        }
    }

    private static final Map<String, List<Regra>> TRANSICOES = new HashMap<>();
    private static final Map<String, TipoToken> FINAIS = new HashMap<>();

    private static int total = 0;
    private static int falhas = 0;

    static {
        monta("q0", r(WS, "q0"), r(NL, "q0"), r(LETRA, "q1"), r(DIGITO, "q2"),
                r("+", "q8"), r("-", "q9"), r("*", "q10"), r("/", "q11"), r("=", "q12"),
                r(">", "q13"), r("<", "q15"), r("(", "q18"), r(")", "q19"), r(",", "q20"),
                r(";", "q21"), r(":", "q22"), r(".", "q24"), r("\"", "q25"), r("'", "q27"),
                r(OUTRO, "q34"));
        monta("q1", r(LETRA, "q1"), r(DIGITO, "q1"), r("_", "q1"));
        monta("q2", r(DIGITO, "q2"), r(".", "q3"));
        monta("q3", r(DIGITO, "q4"));
        monta("q4", r(DIGITO, "q4"), r("e", "q5"), r("E", "q5"));
        monta("q5", r("+", "q6"), r("-", "q6"), r(DIGITO, "q7"));
        monta("q6", r(DIGITO, "q7"));
        monta("q7", r(DIGITO, "q7"));
        monta("q11", r("*", "q31"));
        monta("q13", r("=", "q14"));
        monta("q15", r("=", "q16"), r(">", "q17"));
        monta("q22", r("=", "q23"));
        monta("q25", r("\"", "q26"), s(NL, "q35"), s(EOF, "q35"), r(OUTRO, "q25"));
        monta("q27", s("'", "q36"), s(NL, "q36"), s(EOF, "q36"), r(OUTRO, "q28"));
        monta("q28", r("'", "q29"), s(OUTRO, "q36"));
        monta("q31", r("*", "q32"), s(EOF, "q37"), r(OUTRO, "q31"));
        monta("q32", r("/", "q0"), r("*", "q32"), s(EOF, "q37"), r(OUTRO, "q31"));

        FINAIS.put("q1", TipoToken.IDENTIFICADOR);
        FINAIS.put("q2", TipoToken.NUMERO_INTEIRO);
        FINAIS.put("q4", TipoToken.NUMERO_REAL);
        FINAIS.put("q7", TipoToken.NUMERO_REAL);
        for (String q : Arrays.asList("q8", "q9", "q10", "q11")) {
            FINAIS.put(q, TipoToken.OPERADOR_ARITMETICO);
        }
        for (String q : Arrays.asList("q12", "q13", "q14", "q15", "q16", "q17")) {
            FINAIS.put(q, TipoToken.OPERADOR_RELACIONAL);
        }
        for (String q : Arrays.asList("q18", "q19", "q20", "q21", "q22")) {
            FINAIS.put(q, TipoToken.SIMBOLO_ESPECIAL);
        }
        FINAIS.put("q23", TipoToken.ATRIBUICAO);
        FINAIS.put("q24", TipoToken.FIM);
        FINAIS.put("q26", TipoToken.CONSTANTE_STRING);
        FINAIS.put("q29", TipoToken.CONSTANTE_CHAR);
        for (String q : Arrays.asList("q34", "q35", "q36", "q37")) {
            FINAIS.put(q, TipoToken.ERRO_LEXICO);
        }
    }

    // Transicao que consome o caractere lido.
    private static Regra r(String simbolo, String destino) {
        return new Regra(simbolo, destino, true);
    }

    // Transicao que nao consome o caractere (ele e relido no proximo token).
    private static Regra s(String simbolo, String destino) {
        return new Regra(simbolo, destino, false);
    }

    private static void monta(String estado, Regra... regras) {
        TRANSICOES.put(estado, Arrays.asList(regras));
    }

    public static void main(String[] args) {
        checarEstruturaDoAutomato();
        checarCasosFixos();
        checarIdentificadoresLongos();
        checarEntradasAleatorias();

        System.out.println();
        System.out.println(total + " verificacoes do AFD, " + falhas + " falha(s).");
        if (falhas > 0) {
            System.exit(1);
        }
    }

    private static void checarEstruturaDoAutomato() {
        Set<String> estados = new HashSet<>(TRANSICOES.keySet());
        for (List<Regra> regras : TRANSICOES.values()) {
            for (Regra regra : regras) {
                estados.add(regra.destino);
            }
        }
        checar("AFD tem 36 estados (q0-q29, q31, q32, q34-q37)", estados.size() == 36);
        checar("AFD tem 27 estados finais", FINAIS.size() == 27);
        checar("AFD tem 9 estados nao finais", estados.size() - FINAIS.size() == 9);
    }

    private static void checarCasosFixos() {
        checarCaso("1.a", "1", ".", "a");
        checarCaso("1.5e", "1.5", "e");
        checarCaso("1.5e+", "1.5", "e", "+");
        checarCaso("5e3", "5", "e3");
        checarCaso("24.40e-04", "24.40e-04");
        checarCaso("/* x */ a", "a");
        checarCaso("/* aberto", "/*");
        checarCaso("\"aberta\nx", "\"aberta", "x");
        checarCaso("''", "'", "'");
        checarCaso("'ab'", "'a", "b", "'");
        checarCaso("a:=b<>c", "a", ":=", "b", "<>", "c");
    }

    private static void checarIdentificadoresLongos() {
        String a63 = repetir('a', 63);
        checarCaso(repetir('a', 70), a63);
        checarCaso(a63 + "1", a63);
        checarCaso(repetir('x', 64) + " y", repetir('x', 63), "y");
    }

    private static void checarEntradasAleatorias() {
        String[] alfabeto = {"a", "b", "E", "_", " ", "e", "1", "9", ".", "+", "-", "*", "/", "=",
                "<", ">", ":", ";", ",", "(", ")", "\n", "\"", "'", "@", "\r", "\t", "\f",
                "e", "E", ".", "/", "*", "\"", "'", "\u00e9", "\u00e7", "\u03c0", "\u00f1",
                "1", "0", "\ud83d\ude00"};
        Random sorteio = new Random(2026);
        int quantidade = 300000;
        String primeiraDiferenca = null;
        for (int i = 0; i < quantidade && primeiraDiferenca == null; i++) {
            StringBuilder entrada = new StringBuilder();
            int tamanho = 1 + sorteio.nextInt(18);
            for (int k = 0; k < tamanho; k++) {
                entrada.append(alfabeto[sorteio.nextInt(alfabeto.length)]);
            }
            String texto = entrada.toString();
            if (!tokenizarComAnalisador(texto).equals(tokenizarComModelo(texto))) {
                primeiraDiferenca = texto;
            }
        }
        checar(quantidade + " entradas aleatorias: AFD e analisador geram os mesmos tokens"
                + (primeiraDiferenca == null ? "" : " (primeira diferenca: "
                + escapar(primeiraDiferenca) + ")"), primeiraDiferenca == null);
    }

    // O mesmo texto passa pelo analisador e pelo modelo do AFD; os lexemas
    // tambem precisam bater com a lista esperada.
    private static void checarCaso(String entrada, String... lexemasEsperados) {
        List<String> doAnalisador = tokenizarComAnalisador(entrada);
        List<String> doModelo = tokenizarComModelo(entrada);
        List<String> esperados = new ArrayList<>();
        for (String lexema : lexemasEsperados) {
            esperados.add(lexema);
        }
        boolean ok = doAnalisador.equals(doModelo) && lexemas(doAnalisador).equals(esperados);
        checar("'" + escapar(entrada) + "' -> " + lexemas(doAnalisador), ok);
    }

    private static List<String> lexemas(List<String> pares) {
        List<String> lista = new ArrayList<>();
        for (String par : pares) {
            lista.add(par.substring(0, par.lastIndexOf('\t')));
        }
        return lista;
    }

    private static List<String> tokenizarComAnalisador(String texto) {
        AnalisadorLexico analisador = new AnalisadorLexico(texto, TABELA);
        List<String> pares = new ArrayList<>();
        Token token;
        while ((token = analisador.proximoToken()) != null) {
            pares.add(token.getLexema() + "\t" + token.getTipo().getDescricao());
        }
        return pares;
    }

    private static List<String> tokenizarComModelo(String texto) {
        int[] entrada = texto.codePoints().toArray();
        List<String> pares = new ArrayList<>();
        int inicio = 0;
        while (true) {
            String estado = "q0";
            int atual = inicio;
            String ultimoFinal = null;
            int fimDoUltimoFinal = inicio;
            while (true) {
                int c = atual < entrada.length ? entrada[atual] : FIM_DE_ENTRADA;
                if (estado.equals("q0") && c == FIM_DE_ENTRADA) {
                    return pares;
                }
                Regra regra = proximaRegra(estado, c);
                if (regra == null) {
                    break;
                }
                if (regra.consome) {
                    atual++;
                }
                estado = regra.destino;
                if (estado.equals("q0")) {
                    // espaco ou comentario fechado: descarta e recomeca
                    inicio = atual;
                    ultimoFinal = null;
                    continue;
                }
                if (FINAIS.containsKey(estado)) {
                    ultimoFinal = estado;
                    fimDoUltimoFinal = atual;
                    if (FINAIS.get(estado) == TipoToken.ERRO_LEXICO) {
                        break;
                    }
                }
            }
            if (ultimoFinal == null) {
                throw new IllegalStateException("sem estado final em " + escapar(texto));
            }
            String lexema = new String(entrada, inicio, fimDoUltimoFinal - inicio);
            TipoToken tipo = FINAIS.get(ultimoFinal);
            if (ultimoFinal.equals("q1")) {
                if (lexema.length() > TAMANHO_MAXIMO_IDENTIFICADOR) {
                    lexema = lexema.substring(0, TAMANHO_MAXIMO_IDENTIFICADOR);
                }
                TipoToken reservado = TABELA.buscar(lexema);
                tipo = reservado != null ? reservado : TipoToken.IDENTIFICADOR;
            } else if (ultimoFinal.equals("q37")) {
                lexema = "/*";
            }
            pares.add(lexema + "\t" + tipo.getDescricao());
            inicio = fimDoUltimoFinal;
        }
    }

    private static Regra proximaRegra(String estado, int c) {
        List<Regra> regras = TRANSICOES.get(estado);
        if (regras == null) {
            return null;
        }
        for (Regra regra : regras) {
            if (casa(regra.simbolo, c)) {
                return regra;
            }
        }
        return null;
    }

    private static boolean casa(String simbolo, int c) {
        if (simbolo.equals(EOF)) {
            return c == FIM_DE_ENTRADA;
        }
        if (simbolo.equals(OUTRO)) {
            return true;
        }
        if (simbolo.equals(WS)) {
            return c == ' ' || c == '\t' || c == '\f' || c == 0x0B;
        }
        if (simbolo.equals(NL)) {
            return c == '\n' || c == '\r';
        }
        if (simbolo.equals(LETRA)) {
            return ehLetraLatina(c);
        }
        if (simbolo.equals(DIGITO)) {
            return c >= '0' && c <= '9';
        }
        return c == simbolo.charAt(0);
    }

    // Letras do alfabeto latino, com ou sem acento, no plano basico do Unicode.
    private static boolean ehLetraLatina(int c) {
        return c >= 0 && c <= 0xFFFF && Character.isLetter(c)
                && Character.UnicodeScript.of(c) == Character.UnicodeScript.LATIN;
    }

    private static String repetir(char c, int vezes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vezes; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String escapar(String texto) {
        return texto.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\f", "\\f");
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
