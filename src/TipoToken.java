/**
 * Enumeração de todos os tipos de tokens possíveis na linguagem.
 */
public enum TipoToken {
    // Palavras-chave
    INICIO_PROGRAMA, FIM_PROGRAMA, TIPO_INTEIRO, TIPO_REAL, TIPO_CARACTER,
    SE, ENTAO, SENAO, ENQUANTO,
    // Identificadores e Literais
    IDENTIFICADOR, NUMERO,
    // Operadores e Símbolos
    OP_SOMA, OP_MULT, OP_DIV, OP_RESTO,
    OP_BOOLEANO_E, OP_BOOLEANO_OR, OP_BOOLEANO_NOT,
    OP_ATRIBUICAO, OP_LOGICO,
    PONTO_E_VIRGULA, VIRGULA, ABRE_PARENTESES, FECHA_PARENTESES,
    EOF
}