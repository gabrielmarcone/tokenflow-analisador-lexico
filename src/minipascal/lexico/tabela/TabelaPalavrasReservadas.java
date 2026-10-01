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

        // operador aritmetico
        tabela.put("mod", TipoToken.OPERADOR_ARITMETICO);

        tabela.put("and", TipoToken.OPERADOR_LOGICO);
        tabela.put("or", TipoToken.OPERADOR_LOGICO);
        tabela.put("not", TipoToken.OPERADOR_LOGICO);
    }

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