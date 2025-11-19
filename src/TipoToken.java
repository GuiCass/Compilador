public enum TipoToken {
	
    // Palavras-chave
    INICIO_PROGRAMA, FIM_PROGRAMA, TIPO_INTEIRO, TIPO_REAL, TIPO_CARACTER,
    SE, ENTAO, SENAO, ENQUANTO,

    // Identificadores e Literais
    IDENTIFICADOR, NUMERO,

    // Operadores
    OP_SOMA, OP_MULT, OP_DIV, OP_RESTO, // +, *, /, RESTO
    OP_ATRIBUICAO, // =
    OP_LOGICO, // >, <, ==, <=, >=, !=

    // Símbolos
    PONTO_E_VIRGULA, VIRGULA, ABRE_PARENTESES, FECHA_PARENTESES,

    // Fim do arquivo
    EOF
}