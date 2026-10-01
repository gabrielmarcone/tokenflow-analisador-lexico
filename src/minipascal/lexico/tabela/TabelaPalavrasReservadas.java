package minipascal.lexico.tabela;

import minipascal.lexico.model.TipoToken;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tabela de palavras reservadas e operadores de palavra (mod, and, or, not),
 * montada uma única vez no início da execução. Busca por HashMap, O(1).
 */
public class TabelaPalavrasReservadas {

    private final Map<String, TipoToken> tabela;

    public TabelaPalavrasReservadas() {
        this.tabela = new HashMap<>();
        construirTabela();
    }

    private void construirTabela() {
        // Lista do enunciado. "nit" e "dowto" estao assim la (provaveis erros de
        // digitacao de "nil" e "downto") e foram mantidos; "downto" tambem entra
        // por ser a grafia correta do Pascal.
        String[] palavrasReservadas = {
            "absolute", "array", "begin", "case", "char", "const", "div", "do",
            "downto", "dowto", "else", "end", "external", "file", "for", "forward", "func",
            "function", "goto", "if", "implementation", "integer", "interface",
            "interrupt", "label", "main", "nil", "nit", "of", "packed", "proc",
            "program", "real", "record", "repeat", "set", "shl", "shr", "string",
            "then", "to", "type", "unit", "until", "uses", "var", "while", "with",
            "xor"
        };
        for (String palavra : palavrasReservadas) {
            tabela.put(palavra, TipoToken.PALAVRA_RESERVADA);
        }

        // Nao estao na lista de palavras reservadas, mas o enunciado os trata como
        // operadores: mod e aritmetico; and, or e not sao logicos.
        tabela.put("mod", TipoToken.OPERADOR_ARITMETICO);

        tabela.put("and", TipoToken.OPERADOR_LOGICO);
        tabela.put("or", TipoToken.OPERADOR_LOGICO);
        tabela.put("not", TipoToken.OPERADOR_LOGICO);
    }

    // Retorna null se o lexema nao esta na tabela (ou seja, e identificador).
    // Locale.ROOT garante o mesmo resultado em qualquer idioma do sistema; no
    // turco, por exemplo, "I".toLowerCase() viraria "i" sem ponto e nao acharia "integer".
    public TipoToken buscar(String lexema) {
        return tabela.get(lexema.toLowerCase(Locale.ROOT));
    }

    public boolean contem(String lexema) {
        return buscar(lexema) != null;
    }

    public int tamanho() {
        return tabela.size();
    }
}