package minipascal.lexico.verificacao;

import minipascal.lexico.model.Token;
import minipascal.lexico.model.TipoToken;
import minipascal.lexico.tabela.TabelaPalavrasReservadas;

/**
 * Verificação manual da Fase 3, sem JUnit — dá pra apagar quando os testes
 * de verdade estiverem configurados na IDE.
 */
public class VerificacaoFase3 {

    private static int total = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        TabelaPalavrasReservadas tabela = new TabelaPalavrasReservadas();

        checar("'program' deve ser Palavra reservada",
                tabela.buscar("program") == TipoToken.PALAVRA_RESERVADA);

        checar("'div' deve ser Palavra reservada",
                tabela.buscar("div") == TipoToken.PALAVRA_RESERVADA);
        checar("'Program' (maiúscula) também deve ser Palavra reservada",
                tabela.buscar("Program") == TipoToken.PALAVRA_RESERVADA);
        checar("'PROGRAM' (tudo maiúsculo) também deve ser Palavra reservada",
                tabela.buscar("PROGRAM") == TipoToken.PALAVRA_RESERVADA);

        checar("'downto' deve ser Palavra reservada",
                tabela.buscar("downto") == TipoToken.PALAVRA_RESERVADA);
        checar("'dowto' (grafia do enunciado) não é reservada, é identificador",
                tabela.buscar("dowto") == null);

        checar("'mod' deve ser Operador aritmetico",
                tabela.buscar("mod") == TipoToken.OPERADOR_ARITMETICO);

        checar("'and' deve ser Operador logico",
                tabela.buscar("and") == TipoToken.OPERADOR_LOGICO);
        checar("'or' deve ser Operador logico",
                tabela.buscar("or") == TipoToken.OPERADOR_LOGICO);
        checar("'not' deve ser Operador logico",
                tabela.buscar("not") == TipoToken.OPERADOR_LOGICO);

        checar("'total' deve retornar null (não é reservada)",
                tabela.buscar("total") == null);
        checar("'Media_das_medias' deve retornar null (não é reservada)",
                tabela.buscar("Media_das_medias") == null);

        checar("tabela deve ter 52 entradas (48 reservadas, mod, and, or, not)",
                tabela.tamanho() == 52);

        Token t = new Token("program", TipoToken.PALAVRA_RESERVADA, 1);
        checar("Token.toString() deve ser 'program\\tPalavra reservada'",
                t.toString().equals("program\tPalavra reservada"));

        Token t2 = new Token("pi", TipoToken.IDENTIFICADOR, 3);
        checar("getLexema() deve retornar 'pi'", t2.getLexema().equals("pi"));
        checar("getLinha() deve retornar 3", t2.getLinha() == 3);

        System.out.println();
        System.out.println(total + " verificações, " + falhas + " falha(s).");
        if (falhas > 0) {
            System.exit(1);
        }
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