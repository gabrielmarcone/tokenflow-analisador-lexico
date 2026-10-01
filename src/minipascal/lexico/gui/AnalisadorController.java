package minipascal.lexico.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import minipascal.lexico.core.AnalisadorLexico;
import minipascal.lexico.io.EscritorArquivoSaida;
import minipascal.lexico.io.LeitorArquivoFonte;
import minipascal.lexico.model.Token;
import minipascal.lexico.model.TipoToken;
import minipascal.lexico.tabela.TabelaPalavrasReservadas;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AnalisadorController implements Initializable {

    @FXML private Button btnAbrirArquivo;
    @FXML private Label labelArquivoAtual;
    @FXML private Button btnExecutar;

    @FXML private TextArea areaCodigoFonte;

    @FXML private TableView<Token> tabelaTokens;
    @FXML private TableColumn<Token, Number> colNumero;
    @FXML private TableColumn<Token, String> colLexema;
    @FXML private TableColumn<Token, String> colTipo;
    @FXML private TableColumn<Token, Number> colLinha;

    @FXML private TableView<Token> tabelaErros;
    @FXML private TableColumn<Token, Number> colErroLinha;
    @FXML private TableColumn<Token, String> colErroLexema;
    @FXML private TableColumn<Token, String> colErroDescricao;

    @FXML private Label labelContagemTokens;
    @FXML private Label labelContagemErros;
    @FXML private Label labelStatus;
    @FXML private Button btnSalvarSaida;

    private String nomeArquivoCarregado;
    private ObservableList<Token> ultimosTokens;
    private final TabelaPalavrasReservadas tabela = new TabelaPalavrasReservadas();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarColunas(colNumero, colLexema, colTipo, colLinha);
        configurarColunas(colErroLinha, colErroLexema, colErroDescricao);

        // O botão de executar reflete o conteúdo atual da área de código,
        // não mais só o fato de ter carregado um arquivo — assim tanto faz
        // se o texto veio de um .txt ou foi digitado/colado na hora.
        btnExecutar.setDisable(true);
        areaCodigoFonte.textProperty().addListener((obs, textoAntigo, textoNovo) ->
                btnExecutar.setDisable(textoNovo == null || textoNovo.trim().isEmpty()));
    }

    // Sobrecarga para a tabela principal (4 colunas: #, Lexema, Tipo, Linha).
    private void configurarColunas(TableColumn<Token, Number> numero,
                                    TableColumn<Token, String> lexema,
                                    TableColumn<Token, String> tipo,
                                    TableColumn<Token, Number> linha) {
        numero.setCellValueFactory(dados ->
                new SimpleIntegerProperty(tabelaTokens.getItems().indexOf(dados.getValue()) + 1));
        lexema.setCellValueFactory(dados ->
                new SimpleStringProperty(dados.getValue().getLexema()));
        tipo.setCellValueFactory(dados ->
                new SimpleStringProperty(dados.getValue().getTipo().getDescricao()));
        linha.setCellValueFactory(dados ->
                new SimpleIntegerProperty(dados.getValue().getLinha()));
    }

    // Sobrecarga para a tabela de erros (3 colunas: Linha, Lexema, Descrição).
    private void configurarColunas(TableColumn<Token, Number> linha,
                                    TableColumn<Token, String> lexema,
                                    TableColumn<Token, String> descricao) {
        linha.setCellValueFactory(dados ->
                new SimpleIntegerProperty(dados.getValue().getLinha()));
        lexema.setCellValueFactory(dados ->
                new SimpleStringProperty(dados.getValue().getLexema()));
        descricao.setCellValueFactory(dados ->
                new SimpleStringProperty(descreverErro(dados.getValue())));
    }

    // O lexema do erro indica o que deu errado: o analisador devolve a string
    // ou o char parcial, o "/*" do comentário aberto, ou o caractere isolado.
    private static String descreverErro(Token erro) {
        String lexema = erro.getLexema();
        if (lexema.equals("/*")) {
            return "Comentário não fechado (falta */)";
        }
        if (lexema.startsWith("\"")) {
            return "String não fechada (falta \")";
        }
        if (lexema.startsWith("'")) {
            return "Char deve ter 1 caractere";
        }
        return "Caractere inválido";
    }

    @FXML
    private void abrirArquivo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar arquivo-fonte MiniPascal");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Arquivos de texto (*.txt)", "*.txt"),
                new FileChooser.ExtensionFilter("Todos os arquivos", "*.*")
        );

        File arquivo = chooser.showOpenDialog(btnAbrirArquivo.getScene().getWindow());
        if (arquivo == null) {
            // Usuário cancelou o FileChooser — não é erro, apenas não faz nada.
            return;
        }

        try {
            String conteudo = LeitorArquivoFonte.ler(arquivo.getAbsolutePath());
            areaCodigoFonte.setText(conteudo);
            nomeArquivoCarregado = arquivo.getName();
            labelArquivoAtual.setText(arquivo.getName());
            labelStatus.setText("Arquivo carregado");
        } catch (IOException e) {
            labelStatus.setText("Erro ao ler arquivo — " + e.getMessage());
            nomeArquivoCarregado = null;
        }
    }

    @FXML
    private void executarAnalise() {
        String fonte = areaCodigoFonte.getText();
        if (fonte == null || fonte.trim().isEmpty()) {
            labelStatus.setText("Não há código para analisar");
            return;
        }

        AnalisadorLexico lexer = new AnalisadorLexico(fonte, tabela);

        ObservableList<Token> tokens = FXCollections.observableArrayList();
        Token token;
        while ((token = lexer.proximoToken()) != null) {
            tokens.add(token);
        }

        ultimosTokens = tokens;
        tabelaTokens.setItems(ultimosTokens);

        ObservableList<Token> erros = FXCollections.observableArrayList();
        for (Token t : ultimosTokens) {
            if (t.getTipo() == TipoToken.ERRO_LEXICO) {
                erros.add(t);
            }
        }
        tabelaErros.setItems(erros);

        labelContagemTokens.setText("Tokens: " + ultimosTokens.size());
        labelContagemErros.setText("Erros: " + erros.size());

        if (erros.isEmpty()) {
            labelStatus.setText("Análise concluída — sem erros");
        } else {
            labelStatus.setText("Análise concluída — " + erros.size() + " erro(s) léxico(s)");
        }
        btnSalvarSaida.setDisable(false);
    }

    @FXML
    private void salvarSaida() {
        if (ultimosTokens == null || ultimosTokens.isEmpty()) {
            labelStatus.setText("Nenhum resultado para salvar");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar saída da análise léxica");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos de texto (*.txt)", "*.txt"));
        chooser.setInitialFileName(sugerirNomeSaida());

        File destino = chooser.showSaveDialog(btnSalvarSaida.getScene().getWindow());
        if (destino == null) {
            // Usuário cancelou o FileChooser — não é erro, mantém o estado atual.
            return;
        }

        try {
            EscritorArquivoSaida.escrever(destino.getAbsolutePath(), ultimosTokens);
            labelStatus.setText("Saída salva em " + destino.getName());
        } catch (IOException e) {
            labelStatus.setText("Erro ao salvar — " + e.getMessage());
        }
    }

    // Sugere "<nomeDoArquivoAberto>_saida.txt", mesma convenção do CLI.
    // Se o código foi digitado na hora (sem carregar arquivo), usa um nome
    // genérico.
    private String sugerirNomeSaida() {
        if (nomeArquivoCarregado == null) {
            return "saida.txt";
        }
        return EscritorArquivoSaida.nomeDeSaidaPara(nomeArquivoCarregado);
    }
}