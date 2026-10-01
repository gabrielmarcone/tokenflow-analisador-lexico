package minipascal.lexico.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Lê o conteúdo de um arquivo-fonte. Tenta UTF-8 estrito; se os bytes não
 * forem UTF-8 válido (arquivo salvo como "ANSI" pelo Bloco de Notas, por
 * exemplo), decodifica como ISO-8859-1 para não perder a acentuação.
 * Arquivos UTF-16 com BOM também são reconhecidos.
 */
public class LeitorArquivoFonte {

    private static final char BOM = '﻿';

    public static String ler(String caminho) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(caminho));
        String conteudo = decodificar(bytes);
        if (!conteudo.isEmpty() && conteudo.charAt(0) == BOM) {
            conteudo = conteudo.substring(1);
        }
        return conteudo;
    }

    private static String decodificar(byte[] bytes) {
        // Os bytes FF FE e FE FF (marca de UTF-16) nao sao UTF-8 valido, entao
        // precisam ser testados antes da tentativa de UTF-8.
        if (bytes.length >= 2) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            if (b0 == 0xFF && b1 == 0xFE) {
                return new String(bytes, StandardCharsets.UTF_16LE);
            }
            if (b0 == 0xFE && b1 == 0xFF) {
                return new String(bytes, StandardCharsets.UTF_16BE);
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            // Byte invalido em UTF-8: assume arquivo ANSI (acentos em 1 byte).
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }
}