/*
 * Nome: Henyah Lima do Nascimento
 * RA: 2040482612031
 * Disciplina: Linguagem de Programação I - Fatec Ipiranga - 2026-2
 * Tema 1: Sistema de Controle de Estoque (Elétrica Bom Preço)
 *
 * COMO LER ESTE CÓDIGO (explicação bem simples):
 * - "método" é um pedaço de código com nome. Quando escrevo o nome dele, o Java executa aquele pedaço.
 * - "void" quer dizer que o método faz alguma coisa e não devolve nada.
 * - "int", "double", "boolean" ou "String" antes do nome é o tipo do que o método devolve.
 * - O que fica entre parênteses são os "parâmetros": informações que o método precisa receber.
 * - "return" devolve uma resposta e/ou sai do método na hora.
 */


import java.io.BufferedReader;      // lê um arquivo linha por linha
import java.io.BufferedWriter;      // escreve em um arquivo
import java.io.File;                // representa um arquivo (para saber se ele existe)
import java.io.FileInputStream;     // abre um arquivo para leitura
import java.io.FileOutputStream;    // abre um arquivo para escrita
import java.io.IOException;         // o erro que acontece quando mexer em arquivo dá errado
import java.io.InputStreamReader;   // transforma os bytes lidos em letras
import java.io.OutputStreamWriter;  // transforma letras em bytes para gravar
import java.io.RandomAccessFile;    // permite olhar um ponto específico do arquivo
import java.nio.charset.StandardCharsets; // UTF-8: o "alfabeto" que entende acentos
import java.util.Locale;            // define o jeito de escrever números (vírgula ou ponto)
import java.util.Scanner;           // lê o que é digitado no teclado

public class SistemaEstoque {

    
    // CONSTANTES: valores que nunca mudam ("final" = travado).
    // "static" = pertence ao programa todo, qualquer método enxerga.
   
    static final int MAX_PRODUTOS = 100;        // cabem no máximo 100 produtos
    static final int MAX_MOVIMENTACOES = 500;   // cabem no máximo 500 movimentações
    static final String[] NOMES_CATEGORIAS = {"Elétrica", "Hidráulica", "Ferramentas", "Iluminação", "Diversos"};
    static final String[] NOMES_TIPOS = {"Entrada", "Venda", "Perda/Avaria"};

    static final String ARQ_PRODUTOS = "produtos.txt";
    static final String ARQ_MOVIMENTACOES = "movimentacoes.txt";
    static final String ARQ_REPOSICAO = "reposicao.txt";

    static final String CABECALHO_PRODUTOS = "CODIGO;DESCRICAO;CATEGORIA;QUANTIDADE;PRECO;ESTOQUE_MINIMO;ATIVO";
    static final String CABECALHO_MOVIMENTACOES = "NUMERO;CODIGO_PRODUTO;TIPO;QUANTIDADE;SALDO_APOS";

    static final double PRECO_MAXIMO = 99999.99;
    static final Locale BRASIL = Locale.forLanguageTag("pt-BR"); // mostra 45,90 em vez de 45.90

    
    // ARRAYS PARALELOS DE PRODUTOS
    // Pense em várias gavetas numeradas: a posição i de TODOS os arrays
    // guarda informações do MESMO produto.
    
    static int[] codigos = new int[MAX_PRODUTOS];
    static String[] descricoes = new String[MAX_PRODUTOS];
    static int[] categorias = new int[MAX_PRODUTOS];
    static int[] quantidades = new int[MAX_PRODUTOS];
    static double[] precos = new double[MAX_PRODUTOS];
    static int[] estoquesMinimos = new int[MAX_PRODUTOS];
    static boolean[] ativos = new boolean[MAX_PRODUTOS];
    static int qtdProdutos = 0; // quantas gavetas já estão ocupadas

    // ARRAYS PARALELOS DO HISTÓRICO: a posição m é uma movimentação.
    static int[] movCodigoProduto = new int[MAX_MOVIMENTACOES];
    static int[] movTipo = new int[MAX_MOVIMENTACOES];        // 1 entrada, 2 venda, 3 perda
    static int[] movQuantidade = new int[MAX_MOVIMENTACOES];
    static int[] movSaldoApos = new int[MAX_MOVIMENTACOES];
    static int qtdMovimentacoes = 0;

    // MATRIZ: é uma tabela. Linha = produto; coluna 0 = entrou, 1 = vendeu, 2 = perdeu.
    static int[][] totaisMovimentados = new int[MAX_PRODUTOS][3];

    // Vira "false" se produtos.txt existir mas não puder ser lido.
    // Serve para NÃO apagar o arquivo gravando uma lista vazia por cima dele.
    static boolean podeGravarProdutos = true;

    // Um único leitor de teclado para o programa inteiro.
    // A parte complicada só descobre qual "alfabeto" o terminal usa, para os acentos saírem certos.
    static Scanner sc = new Scanner(System.in,
            System.getProperty("stdin.encoding", System.getProperty("stdout.encoding", "UTF-8")));

    // ==================================================================
    // MAIN: é por aqui que o programa começa.
    // ==================================================================

    /** Carrega os dados (RF15) e fica repetindo o menu até o usuário digitar 0. */
    public static void main(String[] args) {
        carregarDados(); // roda uma vez, antes do primeiro menu

        int opcao;
        do { // "do...while" = faça pelo menos uma vez e repita enquanto a condição for verdadeira
            exibirMenu();
            opcao = lerInteiro("Escolha uma opção", 0, 14);

            // "switch" = escolhe o que executar conforme o número digitado.
            // "break" = terminou este caso, sai do switch.
            switch (opcao) {
                case 1:
                    cadastrarProduto();
                    break;
                case 2:
                    alterarProduto();
                    break;
                case 3:
                    inativarProduto();
                    break;
                case 4:
                    listarProdutos();
                    break;
                case 5:
                    consultarProduto();
                    break;
                case 6:
                    pesquisarPorCategoria();
                    break;
                case 7:
                    registrarEntrada();
                    break;
                case 8: // 2 = venda
                    registrarSaida(2);
                    break;
                case 9: // 3 = perda
                    registrarSaida(3);
                    break;
                case 10:
                    exibirHistorico();
                    break;
                case 11:
                    relatorioEstoqueMinimo();
                    break;
                case 12:
                    relatorioValorEstoque();
                    break;
                case 13:
                    relatorioMovimentacoes();
                    break;
                case 14:
                    exportarReposicao();
                    break;
                default: // qualquer outro valor; aqui só sobra o 0
                    encerrar();
            }
        } while (opcao != 0);
    }

    /** Só desenha o menu na tela. */
    static void exibirMenu() {
        System.out.println();
        System.out.println("=========== ELÉTRICA BOM PREÇO - CONTROLE DE ESTOQUE ===========");
        System.out.println(" CADASTRO          1 - Cadastrar produto");
        System.out.println("                   2 - Alterar produto");
        System.out.println("                   3 - Inativar produto");
        System.out.println(" CONSULTAS         4 - Listar produtos");
        System.out.println("                   5 - Consultar produto por código");
        System.out.println("                   6 - Pesquisar produtos por categoria");
        System.out.println(" MOVIMENTAÇÕES     7 - Registrar entrada de mercadoria");
        System.out.println("                   8 - Registrar saída por venda");
        System.out.println("                   9 - Registrar saída por perda/avaria");
        System.out.println("                  10 - Histórico de movimentações de um produto");
        System.out.println(" RELATÓRIOS       11 - Produtos abaixo do estoque mínimo");
        System.out.println("                  12 - Valor total em estoque");
        System.out.println("                  13 - Resumo de movimentações por produto");
        System.out.println(" ARQUIVOS         14 - Exportar lista de reposição (reposicao.txt)");
        System.out.println("                   0 - Sair");
        System.out.println("================================================================");
    }

    /** RF16: ao sair, grava os produtos uma última vez. */
    static void encerrar() {
        if (salvarProdutos()) {
            System.out.println("Dados gravados em " + ARQ_PRODUTOS + ".");
        }
        System.out.println("Programa encerrado. Até logo!");
    }

    // ==================================================================
    // LEITURA DO TECLADO COM VALIDAÇÃO
    // ==================================================================

    /** Lê uma linha digitada. Se a entrada for fechada (Ctrl+Z / Ctrl+D), sai sem dar erro. */
    static String lerLinha() {
        if (!sc.hasNextLine()) {
            System.out.println("\nEntrada encerrada.");
            encerrar();
            System.exit(0); // desliga o programa
        }
        return sc.nextLine().trim(); // trim() tira os espaços do começo e do fim
    }

    /** RN03 e VG10: pede um texto até ele não ser vazio e não ter ";" (que separa campos no arquivo). */
    static String lerDescricao(String rotulo) {
        while (true) { // repete para sempre, até o "return" tirar a gente daqui
            System.out.print(rotulo + ": ");
            String texto = lerLinha();
            if (texto.isEmpty()) {
                System.out.println("ERRO: a descrição é obrigatória.");
            } else if (texto.contains(";")) {
                System.out.println("ERRO: não use o caractere \";\".");
            } else {
                return texto;
            }
        }
    }

    /** Pede um número inteiro entre "minimo" e "maximo" e só devolve quando estiver certo. */
    static int lerInteiro(String rotulo, int minimo, int maximo) {
        while (true) {
            System.out.print(rotulo + ": ");
            try { // "tente fazer isto..."
                int valor = Integer.parseInt(lerLinha()); // transforma o texto em número
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.println("ERRO: digite um valor de " + minimo + " a " + maximo + ".");
            } catch (NumberFormatException e) { // "...se o texto não for número, faça isto"
                System.out.println("ERRO: digite um número inteiro válido.");
            }
        }
    }

    /** RN05: pede um preço maior que zero e até 99999,99. Aceita vírgula ou ponto. */
    static double lerPreco(String rotulo) {
        while (true) {
            System.out.print(rotulo + ": ");
            try {
                double valor = Double.parseDouble(lerLinha().replace(",", "."));
                if (valor > 0 && valor <= PRECO_MAXIMO) {
                    return valor;
                }
                System.out.println("ERRO: o preço deve ser maior que zero e no máximo R$ 99999,99.");
            } catch (NumberFormatException e) {
                System.out.println("ERRO: digite um valor decimal válido.");
            }
        }
    }

    /** RN04: mostra a lista de categorias e pede um número de 1 a 5. */
    static int lerCategoria() {
        System.out.print("Categorias:");
        for (int c = 0; c < NOMES_CATEGORIAS.length; c++) { // "for" = repete contando
            System.out.print(" " + (c + 1) + "-" + NOMES_CATEGORIAS[c]);
        }
        System.out.println();
        return lerInteiro("Categoria", 1, NOMES_CATEGORIAS.length);
    }

    /** Pergunta S ou N. Devolve true se a resposta for S. */
    static boolean lerSimNao(String pergunta) {
        while (true) {
            System.out.print(pergunta + " (S/N): ");
            String resposta = lerLinha();
            if (resposta.equalsIgnoreCase("S")) return true;  // IgnoreCase: "s" e "S" valem igual
            if (resposta.equalsIgnoreCase("N")) return false;
            System.out.println("ERRO: digite S ou N.");
        }
    }

    // ==================================================================
    // MÉTODOS AUXILIARES (ajudantes usados por vários requisitos)
    // ==================================================================

    /** Procura o código gaveta por gaveta. Devolve a posição, ou -1 se não achar. */
    static int buscarIndiceProduto(int codigo) {
        for (int i = 0; i < qtdProdutos; i++) {
            if (codigos[i] == codigo) {
                return i;
            }
        }
        return -1; // -1 não é posição válida, então significa "não achei"
    }

    /** RN10: lê um código e só aceita produto que existe e está ativo. Devolve a posição ou -1. */
    static int buscarProdutoAtivo() {
        int codigo = lerInteiro("Código do produto", 1000, 9999);
        int i = buscarIndiceProduto(codigo);
        if (i == -1) {
            System.out.println("ERRO: produto não encontrado.");
            return -1;
        }
        if (!ativos[i]) { // "!" significa "não": se NÃO está ativo
            System.out.println("ERRO: o produto está inativo.");
            return -1;
        }
        return i;
    }

    /** Troca o número da categoria (1 a 5) pelo nome. O -1 é porque o array começa na posição 0. */
    static String nomeCategoria(int categoria) {
        return NOMES_CATEGORIAS[categoria - 1];
    }

    /** RN12: diz se o estoque do produto está ZERADO, ABAIXO DO MÍNIMO ou NORMAL. */
    static String situacaoEstoque(int indice) {
        if (quantidades[indice] == 0) return "ZERADO";
        if (quantidades[indice] < estoquesMinimos[indice]) return "ABAIXO DO MÍNIMO";
        return "NORMAL";
    }

    /** RN14: quanto comprar = 2 x mínimo - quantidade atual. Se der zero ou menos, devolve 0. */
    static int sugestaoReposicao(int indice) {
        int sugestao = 2 * estoquesMinimos[indice] - quantidades[indice];
        if (sugestao > 0) return sugestao;
        return 0;
    }

    /** RN14: dinheiro parado naquele produto = quantidade x preço. */
    static double valorEmEstoque(int indice) {
        return quantidades[indice] * precos[indice];
    }

    /** VG09: avisa se não existe nenhum produto cadastrado. Devolve true quando está vazio. */
    static boolean cadastroVazio() {
        if (qtdProdutos == 0) {
            System.out.println("Nenhum produto cadastrado.");
            return true;
        }
        return false;
    }

    /** Imprime os títulos das colunas da tabela. No printf, %-8s = texto em 8 espaços, à esquerda. */
    static void exibirCabecalhoProdutos() {
        System.out.printf("%-7s %-26s %-12s %5s %12s %6s  %s%n",
                "Código", "Descrição", "Categoria", "Qtd", "Preço (R$)", "Mínimo", "Situação");
    }

    /** Imprime UMA linha da tabela. %d = inteiro, %.2f = decimal com 2 casas, %n = pular linha. */
    static void exibirLinhaProduto(int indice) {
        System.out.printf(BRASIL, "%-7d %-26s %-12s %5d %12.2f %6d  %s%n",
                codigos[indice], descricoes[indice], nomeCategoria(categorias[indice]),
                quantidades[indice], precos[indice], estoquesMinimos[indice], situacaoEstoque(indice));
    }

    // ==================================================================
    // CADASTRO (RF01, RF02, RF03)
    // ==================================================================

    /** RF01: cria um produto novo na próxima gaveta livre. */
    static void cadastrarProduto() {
        System.out.println("--- CADASTRAR PRODUTO ---");
        if (qtdProdutos >= MAX_PRODUTOS) { // VG04: não cabe mais
            System.out.println("ERRO: limite de " + MAX_PRODUTOS + " produtos atingido.");
            return;
        }
        int codigo = lerInteiro("Código (1000 a 9999)", 1000, 9999); // RN02
        if (buscarIndiceProduto(codigo) != -1) { // RN01: código repetido não pode
            System.out.println("ERRO: já existe um produto com o código " + codigo + ". Nada foi gravado.");
            return;
        }

        // Primeiro lê tudo, depois guarda.
        String descricao = lerDescricao("Descrição");
        int categoria = lerCategoria();
        double preco = lerPreco("Preço unitário (R$)");
        int minimo = lerInteiro("Estoque mínimo", 0, 9999); // RN06

        int i = qtdProdutos; // próxima gaveta vazia
        codigos[i] = codigo;
        descricoes[i] = descricao;
        categorias[i] = categoria;
        precos[i] = preco;
        estoquesMinimos[i] = minimo;
        quantidades[i] = 0; // RN07: nasce com zero
        ativos[i] = true;   // RN07: nasce ativo
        for (int t = 0; t < 3; t++) totaisMovimentados[i][t] = 0; // zera a linha da matriz
        qtdProdutos++; // ++ = soma 1

        if (salvarProdutos()) { // RN16: só comemora depois de gravar
            System.out.println("Produto cadastrado com sucesso! Quantidade inicial: 0.");
            System.out.println("Dados gravados em " + ARQ_PRODUTOS + ".");
        }
    }

    /** RF02: muda os dados de um produto ativo (código e quantidade não mudam: RN07). */
    static void alterarProduto() {
        System.out.println("--- ALTERAR PRODUTO ---");
        if (cadastroVazio()) return;
        int i = buscarProdutoAtivo();
        if (i == -1) return;

        System.out.println("Dados atuais:");
        exibirCabecalhoProdutos();
        exibirLinhaProduto(i);

        System.out.println("Informe os novos dados:");
        String descricao = lerDescricao("Nova descrição");
        int categoria = lerCategoria();
        double preco = lerPreco("Novo preço unitário (R$)");
        int minimo = lerInteiro("Novo estoque mínimo", 0, 9999);

        descricoes[i] = descricao;
        categorias[i] = categoria;
        precos[i] = preco;
        estoquesMinimos[i] = minimo;

        if (salvarProdutos()) {
            System.out.println("Produto alterado com sucesso!");
        }
    }

    /** RF03: "exclusão lógica": o produto não é apagado, só fica marcado como inativo. */
    static void inativarProduto() {
        System.out.println("--- INATIVAR PRODUTO ---");
        if (cadastroVazio()) return;
        int i = buscarProdutoAtivo();
        if (i == -1) return;

        if (quantidades[i] > 0) { // RN11: só inativa se estiver zerado
            System.out.println("ERRO: o produto possui " + quantidades[i]
                    + " unidade(s) em estoque. Só é possível inativar produtos com quantidade zero.");
            return;
        }
        if (!lerSimNao("Confirma a inativação de \"" + descricoes[i] + "\"?")) {
            System.out.println("Operação cancelada.");
            return;
        }
        ativos[i] = false;
        if (salvarProdutos()) {
            System.out.println("Produto inativado.");
        }
    }

    // ==================================================================
    // CONSULTAS (RF04, RF05, RF06)
    // ==================================================================

    /** RF04: mostra uma tabela com todos os produtos ativos. */
    static void listarProdutos() {
        System.out.println("--- LISTA DE PRODUTOS ---");
        if (cadastroVazio()) return;
        int total = 0;
        exibirCabecalhoProdutos();
        for (int i = 0; i < qtdProdutos; i++) {
            if (ativos[i]) { // RN11: inativo não aparece
                exibirLinhaProduto(i);
                total++;
            }
        }
        System.out.println("Total de produtos ativos: " + total);
    }

    /** RF05: mostra a "ficha" completa de um produto (ativo ou inativo). */
    static void consultarProduto() {
        System.out.println("--- CONSULTAR PRODUTO ---");
        if (cadastroVazio()) return;
        int i = buscarIndiceProduto(lerInteiro("Código do produto", 1000, 9999));
        if (i == -1) {
            System.out.println("Produto não encontrado.");
            return;
        }
        // condição ? valorSeSim : valorSeNao  -> um "if" escrito em uma linha só
        String status = ativos[i] ? "ATIVO" : "INATIVO";

        System.out.println("Código ............: " + codigos[i]);
        System.out.println("Descrição .........: " + descricoes[i]);
        System.out.println("Categoria .........: " + nomeCategoria(categorias[i]));
        System.out.printf(BRASIL, "Preço unitário ....: R$ %.2f%n", precos[i]);
        System.out.println("Quantidade ........: " + quantidades[i] + "      Estoque mínimo: " + estoquesMinimos[i]);
        System.out.println("Situação ..........: " + status + "   Estoque: " + situacaoEstoque(i));
        System.out.printf(BRASIL, "Valor em estoque ..: R$ %.2f%n", valorEmEstoque(i));
        System.out.println("Movimentado .......: Entradas " + totaisMovimentados[i][0]
                + " | Vendas " + totaisMovimentados[i][1] + " | Perdas " + totaisMovimentados[i][2]);
    }

    /** RF06: lista só os produtos ativos da categoria escolhida. */
    static void pesquisarPorCategoria() {
        System.out.println("--- PESQUISAR POR CATEGORIA ---");
        if (cadastroVazio()) return;
        int categoria = lerCategoria();
        int encontrados = 0;
        for (int i = 0; i < qtdProdutos; i++) {
            if (ativos[i] && categorias[i] == categoria) { // "&&" = as duas coisas precisam ser verdade
                if (encontrados == 0) exibirCabecalhoProdutos(); // cabeçalho só antes do primeiro
                exibirLinhaProduto(i);
                encontrados++;
            }
        }
        if (encontrados == 0) {
            System.out.println("Nenhum produto ativo nesta categoria.");
        } else {
            System.out.println("Produtos encontrados: " + encontrados);
        }
    }

    // ==================================================================
    // MOVIMENTAÇÕES (RF07 a RF10)
    // ==================================================================

    /** RF07: chegou mercadoria do fornecedor, então a quantidade aumenta. */
    static void registrarEntrada() {
        System.out.println("--- REGISTRAR ENTRADA DE MERCADORIA ---");
        if (cadastroVazio()) return;
        int i = buscarProdutoAtivo();
        if (i == -1) return;

        System.out.println("Produto: " + descricoes[i] + " | Saldo atual: " + quantidades[i]);
        int quantidade = lerInteiro("Quantidade recebida", 1, 9999); // RN08
        int saldoAnterior = quantidades[i];

        if (registrarMovimentacao(i, 1, quantidade)) {
            System.out.println("Entrada registrada (movimentação nº " + qtdMovimentacoes + "). Saldo anterior: "
                    + saldoAnterior + " | Novo saldo: " + quantidades[i]);
            avisarGravacao();
        }
    }

    /** RF08 (tipo 2 = venda) e RF09 (tipo 3 = perda): a quantidade diminui. */
    static void registrarSaida(int tipo) {
        boolean venda = (tipo == 2); // true se for venda, false se for perda
        System.out.println(venda ? "--- REGISTRAR SAÍDA POR VENDA ---" : "--- REGISTRAR SAÍDA POR PERDA/AVARIA ---");
        if (cadastroVazio()) return;
        int i = buscarProdutoAtivo();
        if (i == -1) return;

        System.out.println("Produto: " + descricoes[i] + " | Saldo atual: " + quantidades[i]);
        int quantidade = lerInteiro(venda ? "Quantidade vendida" : "Quantidade perdida", 1, 9999);

        if (quantidade > quantidades[i]) { // RN09: estoque nunca fica negativo
            System.out.println("ERRO: quantidade solicitada (" + quantidade + ") maior que a disponível ("
                    + quantidades[i] + ").");
            System.out.println(venda ? "Venda não registrada." : "Perda não registrada.");
            return;
        }
        int saldoAnterior = quantidades[i];

        if (registrarMovimentacao(i, tipo, quantidade)) {
            double valor = quantidade * precos[i];
            if (venda) {
                System.out.printf(BRASIL, "Venda registrada (movimentação nº %d). Valor da venda: R$ %.2f%n",
                        qtdMovimentacoes, valor);
            } else {
                System.out.printf(BRASIL, "Perda registrada (movimentação nº %d). Prejuízo: R$ %.2f%n",
                        qtdMovimentacoes, valor);
            }
            System.out.println("Saldo anterior: " + saldoAnterior + " | Novo saldo: " + quantidades[i]);

            if (!situacaoEstoque(i).equals("NORMAL")) { // RN12: alerta. Textos se comparam com equals()
                System.out.println("*** ALERTA: produto " + situacaoEstoque(i) + " (mínimo "
                        + estoquesMinimos[i] + "). Providenciar reposição. ***");
            }
            avisarGravacao();
        }
    }

    /**
     * RN13 e RN16: é o ÚNICO lugar que muda a quantidade.
     * Atualiza estoque, histórico, matriz e arquivos. Devolve true só se tudo deu certo.
     */
    static boolean registrarMovimentacao(int indice, int tipo, int quantidade) {
        if (qtdMovimentacoes >= MAX_MOVIMENTACOES) {
            System.out.println("ERRO: histórico cheio (" + MAX_MOVIMENTACOES
                    + " movimentações). Movimentação não realizada.");
            return false;
        }
        if (tipo == 1) {
            quantidades[indice] += quantidade; // += soma no valor que já existe
        } else {
            quantidades[indice] -= quantidade; // -= subtrai
        }

        int m = qtdMovimentacoes;
        movCodigoProduto[m] = codigos[indice];
        movTipo[m] = tipo;
        movQuantidade[m] = quantidade;
        movSaldoApos[m] = quantidades[indice];
        qtdMovimentacoes++;

        totaisMovimentados[indice][tipo - 1] += quantidade; // coluna = tipo - 1

        boolean gravouHistorico = anexarMovimentacao(m);
        boolean gravouProdutos = salvarProdutos();
        if (!gravouHistorico || !gravouProdutos) { // RN16: sem gravação, sem mensagem de sucesso
            System.out.println("ATENÇÃO: a movimentação está na memória, mas NÃO foi totalmente gravada.");
            return false;
        }
        return true;
    }

    /** Avisa quais arquivos foram atualizados depois de uma movimentação. */
    static void avisarGravacao() {
        System.out.println("Movimentação acrescentada a " + ARQ_MOVIMENTACOES + "; "
                + ARQ_PRODUTOS + " atualizado.");
    }

    /** RF10: mostra todas as movimentações de um produto, na ordem em que aconteceram. */
    static void exibirHistorico() {
        System.out.println("--- HISTÓRICO DE MOVIMENTAÇÕES ---");
        if (cadastroVazio()) return;
        int i = buscarIndiceProduto(lerInteiro("Código do produto", 1000, 9999));
        if (i == -1) {
            System.out.println("Produto não encontrado.");
            return;
        }
        System.out.println("Produto: " + descricoes[i]);
        int encontradas = 0;
        for (int m = 0; m < qtdMovimentacoes; m++) {
            if (movCodigoProduto[m] == codigos[i]) {
                if (encontradas == 0) {
                    System.out.printf("%-6s %-14s %10s %12s%n", "Nº", "Tipo", "Quantidade", "Saldo após");
                }
                System.out.printf("%-6d %-14s %10d %12d%n",
                        m + 1, NOMES_TIPOS[movTipo[m] - 1], movQuantidade[m], movSaldoApos[m]);
                encontradas++;
            }
        }
        if (encontradas == 0) {
            System.out.println("Nenhuma movimentação registrada para este produto.");
        }
    }

    // ==================================================================
    // RELATÓRIOS (RF11, RF12, RF13)
    // ==================================================================

    /** RF11: mostra os produtos que precisam ser comprados. */
    static void relatorioEstoqueMinimo() {
        System.out.println("--- PRODUTOS ABAIXO DO ESTOQUE MÍNIMO ---");
        if (cadastroVazio()) return;
        int total = 0;
        for (int i = 0; i < qtdProdutos; i++) {
            if (ativos[i] && !situacaoEstoque(i).equals("NORMAL")) {
                if (total == 0) {
                    System.out.printf("%-7s %-26s %5s %6s  %-17s %8s%n",
                            "Código", "Descrição", "Qtd", "Mínimo", "Situação", "Sugestão");
                }
                System.out.printf("%-7d %-26s %5d %6d  %-17s %8d%n", codigos[i], descricoes[i],
                        quantidades[i], estoquesMinimos[i], situacaoEstoque(i), sugestaoReposicao(i));
                total++;
            }
        }
        if (total == 0) {
            System.out.println("Todos os produtos estão com estoque adequado.");
        } else {
            System.out.println("Produtos que necessitam de reposição: " + total);
        }
    }

    /** RF12: calcula quanto dinheiro existe em estoque, por produto, por categoria e no total. */
    static void relatorioValorEstoque() {
        System.out.println("--- VALOR TOTAL EM ESTOQUE ---");
        if (cadastroVazio()) return;
        double[] totalPorCategoria = new double[NOMES_CATEGORIAS.length]; // 5 "cofrinhos", começam em 0
        double totalGeral = 0;

        System.out.printf("%-7s %-26s %5s %12s %14s%n", "Código", "Descrição", "Qtd", "Preço (R$)", "Valor (R$)");
        for (int i = 0; i < qtdProdutos; i++) {
            if (ativos[i]) {
                double valor = valorEmEstoque(i);
                System.out.printf(BRASIL, "%-7d %-26s %5d %12.2f %14.2f%n",
                        codigos[i], descricoes[i], quantidades[i], precos[i], valor);
                totalPorCategoria[categorias[i] - 1] += valor; // joga no cofrinho da categoria
                totalGeral += valor;
            }
        }
        System.out.println("Totais por categoria:");
        for (int c = 0; c < NOMES_CATEGORIAS.length; c++) {
            System.out.printf(BRASIL, "  %-14s R$ %12.2f%n", NOMES_CATEGORIAS[c], totalPorCategoria[c]);
        }
        System.out.printf(BRASIL, "VALOR TOTAL DO ESTOQUE: R$ %.2f%n", totalGeral);
    }

    /** RF13: mostra a matriz de totais, a soma de cada coluna, o mais vendido e o com mais perda. */
    static void relatorioMovimentacoes() {
        System.out.println("--- RESUMO DE MOVIMENTAÇÕES POR PRODUTO ---");
        if (cadastroVazio()) return;
        int[] totalColuna = new int[3]; // soma de entradas, vendas e perdas
        int totalAtual = 0;
        int maisVendido = -1;  // -1 = ainda não achei ninguém
        int maiorPerda = -1;

        System.out.printf("%-7s %-26s %8s %8s %8s %6s%n",
                "Código", "Descrição", "Entradas", "Vendas", "Perdas", "Atual");
        for (int i = 0; i < qtdProdutos; i++) {
            if (!ativos[i]) continue; // "continue" = pula para o próximo produto

            System.out.printf("%-7d %-26s %8d %8d %8d %6d%n", codigos[i], descricoes[i],
                    totaisMovimentados[i][0], totaisMovimentados[i][1], totaisMovimentados[i][2], quantidades[i]);
            for (int t = 0; t < 3; t++) totalColuna[t] += totaisMovimentados[i][t];
            totalAtual += quantidades[i];

            // "||" = ou. Troca o campeão se este produto for maior que o campeão atual.
            if (totaisMovimentados[i][1] > 0
                    && (maisVendido == -1 || totaisMovimentados[i][1] > totaisMovimentados[maisVendido][1])) {
                maisVendido = i;
            }
            if (totaisMovimentados[i][2] > 0
                    && (maiorPerda == -1 || totaisMovimentados[i][2] > totaisMovimentados[maiorPerda][2])) {
                maiorPerda = i;
            }
        }
        System.out.printf("%-34s %8d %8d %8d %6d%n", "TOTAIS",
                totalColuna[0], totalColuna[1], totalColuna[2], totalAtual);

        if (maisVendido == -1) {
            System.out.println("Não houve vendas.");
        } else {
            System.out.println("Produto mais vendido: " + descricoes[maisVendido]
                    + " (" + totaisMovimentados[maisVendido][1] + " un.)");
        }
        if (maiorPerda == -1) {
            System.out.println("Não houve perdas.");
        } else {
            System.out.println("Produto com maior perda: " + descricoes[maiorPerda]
                    + " (" + totaisMovimentados[maiorPerda][2] + " un.)");
        }
    }

    // ==================================================================
    // EXPORTAÇÃO (RF14)
    // ==================================================================

    /** RF14: grava em reposicao.txt a lista do que precisa ser comprado. */
    static void exportarReposicao() {
        System.out.println("--- EXPORTAR LISTA DE REPOSIÇÃO ---");
        int total = 0; // primeiro só conta, para não criar arquivo vazio
        for (int i = 0; i < qtdProdutos; i++) {
            if (ativos[i] && !situacaoEstoque(i).equals("NORMAL")) total++;
        }
        if (total == 0) {
            System.out.println("Nenhum produto precisa de reposição. O arquivo não foi gerado.");
            return;
        }

        // try com parênteses = o arquivo é fechado sozinho no final, mesmo se der erro.
        // "false" = apaga o conteúdo antigo e escreve por cima (sobrescrita).
        try (BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ARQ_REPOSICAO, false), StandardCharsets.UTF_8))) {
            saida.write("LISTA DE REPOSIÇÃO - ELÉTRICA BOM PREÇO");
            saida.newLine(); // pula linha dentro do arquivo
            saida.write(String.format("%-7s %-26s %-12s %5s %6s  %-17s %8s",
                    "Código", "Descrição", "Categoria", "Qtd", "Mínimo", "Situação", "Sugestão"));
            saida.newLine();

            int unidades = 0;
            for (int i = 0; i < qtdProdutos; i++) {
                if (ativos[i] && !situacaoEstoque(i).equals("NORMAL")) {
                    saida.write(String.format("%-7d %-26s %-12s %5d %6d  %-17s %8d", codigos[i], descricoes[i],
                            nomeCategoria(categorias[i]), quantidades[i], estoquesMinimos[i],
                            situacaoEstoque(i), sugestaoReposicao(i)));
                    saida.newLine();
                    unidades += sugestaoReposicao(i);
                }
            }
            saida.write("Total de unidades sugeridas: " + unidades);
            saida.newLine();
        } catch (IOException e) {
            System.out.println("ERRO ao gravar " + ARQ_REPOSICAO + ": " + e.getMessage());
            return;
        }
        System.out.println("Lista de reposição exportada para " + ARQ_REPOSICAO + " (" + total + " produtos).");
    }

    // ==================================================================
    // CARGA DOS ARQUIVOS (RF15)
    // ==================================================================

    /** RF15: lê os produtos primeiro, depois o histórico, e por fim confere se as contas batem. */
    static void carregarDados() {
        System.out.println("=== CARREGANDO DADOS ===");
        carregarProdutos();
        carregarMovimentacoes();
        if (qtdProdutos > 0 && verificarConsistencia() == 0) {
            System.out.println("Verificação de consistência (RN15): nenhuma inconsistência encontrada.");
        }
    }

    /** RF15 e RN17: lê produtos.txt, ignora linhas ruins e devolve quantos produtos carregou. */
    static int carregarProdutos() {
        File arquivo = new File(ARQ_PRODUTOS);
        if (!arquivo.exists()) { // VG11: primeira execução
            System.out.println("Arquivo " + ARQ_PRODUTOS + " não encontrado. Iniciando com cadastro vazio.");
            return 0;
        }
        int numeroLinha = 0;
        int ignoradas = 0;
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(
                new FileInputStream(arquivo), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) { // null = acabou o arquivo
                numeroLinha++;
                if (numeroLinha == 1 && linha.contains("CODIGO")) continue; // pula o cabeçalho

                if (qtdProdutos >= MAX_PRODUTOS) { // VG04: encheu, para de ler
                    System.out.println(ARQ_PRODUTOS + ": capacidade máxima atingida. Linhas a partir da "
                            + numeroLinha + " não foram carregadas.");
                    break; // "break" = sai do laço
                }
                String erro = carregarLinhaProduto(linha);
                if (!erro.isEmpty()) { // VG12
                    System.out.println(ARQ_PRODUTOS + ": linha " + numeroLinha + " ignorada - " + erro);
                    ignoradas++;
                }
            }
        } catch (IOException e) {
            System.out.println("ERRO ao ler " + ARQ_PRODUTOS + ": " + e.getMessage());
            System.out.println("Por segurança, esse arquivo não será regravado nesta execução.");
            podeGravarProdutos = false;
        }
        System.out.println(ARQ_PRODUTOS + ": " + qtdProdutos + " registro(s) carregado(s), "
                + ignoradas + " linha(s) ignorada(s)");
        return qtdProdutos;
    }

    /** RN17: confere uma linha de produto. Se estiver boa, guarda e devolve ""; se não, devolve o motivo. */
    static String carregarLinhaProduto(String linha) {
        String[] campos = linha.split(";", -1); // corta o texto em cada ";"
        if (campos.length != 7) return "a linha deve ter 7 campos";
        for (int c = 0; c < campos.length; c++) campos[c] = campos[c].trim();

        int codigo = converterInteiro(campos[0]);
        if (codigo < 1000 || codigo > 9999) return "código inválido: \"" + campos[0] + "\"";
        if (buscarIndiceProduto(codigo) != -1) return "código repetido: " + codigo;
        if (campos[1].isEmpty()) return "descrição vazia";
        int categoria = converterInteiro(campos[2]);
        if (categoria < 1 || categoria > 5) return "categoria inválida: \"" + campos[2] + "\"";
        int quantidade = converterInteiro(campos[3]);
        if (quantidade < 0) return "quantidade inválida: \"" + campos[3] + "\"";
        double preco;
        try {
            preco = Double.parseDouble(campos[4]);
        } catch (NumberFormatException e) {
            return "preço inválido: \"" + campos[4] + "\"";
        }
        if (!(preco > 0 && preco <= PRECO_MAXIMO)) return "preço inválido: \"" + campos[4] + "\"";
        int minimo = converterInteiro(campos[5]);
        if (minimo < 0 || minimo > 9999) return "estoque mínimo inválido: \"" + campos[5] + "\"";
        if (!campos[6].equals("true") && !campos[6].equals("false")) {
            return "situação inválida: \"" + campos[6] + "\"";
        }

        // Passou em todos os testes: guarda na próxima gaveta.
        codigos[qtdProdutos] = codigo;
        descricoes[qtdProdutos] = campos[1];
        categorias[qtdProdutos] = categoria;
        quantidades[qtdProdutos] = quantidade;
        precos[qtdProdutos] = preco;
        estoquesMinimos[qtdProdutos] = minimo;
        ativos[qtdProdutos] = campos[6].equals("true");
        qtdProdutos++;
        return "";
    }

    /** Transforma texto em inteiro. Se não for número, devolve -1 (que é sempre inválido aqui). */
    static int converterInteiro(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** RF15 e RN17: lê o histórico, soma na matriz e devolve quantas movimentações carregou. */
    static int carregarMovimentacoes() {
        File arquivo = new File(ARQ_MOVIMENTACOES);
        if (!arquivo.exists()) {
            System.out.println("Arquivo " + ARQ_MOVIMENTACOES + " não encontrado. Iniciando com histórico vazio.");
            return 0;
        }
        int numeroLinha = 0;
        int ignoradas = 0;
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(
                new FileInputStream(arquivo), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                numeroLinha++;
                if (numeroLinha == 1 && linha.contains("NUMERO")) continue;

                if (qtdMovimentacoes >= MAX_MOVIMENTACOES) {
                    System.out.println(ARQ_MOVIMENTACOES + ": capacidade máxima atingida. Linhas a partir da "
                            + numeroLinha + " não foram carregadas.");
                    break;
                }
                String erro = carregarLinhaMovimentacao(linha);
                if (!erro.isEmpty()) {
                    System.out.println(ARQ_MOVIMENTACOES + ": linha " + numeroLinha + " ignorada - " + erro);
                    ignoradas++;
                }
            }
        } catch (IOException e) {
            System.out.println("ERRO ao ler " + ARQ_MOVIMENTACOES + ": " + e.getMessage());
        }
        System.out.println(ARQ_MOVIMENTACOES + ": " + qtdMovimentacoes + " registro(s) carregado(s), "
                + ignoradas + " linha(s) ignorada(s)");
        return qtdMovimentacoes;
    }

    /** RN17: confere uma linha do histórico. Devolve "" se deu certo, ou o motivo do erro. */
    static String carregarLinhaMovimentacao(String linha) {
        String[] campos = linha.split(";", -1);
        if (campos.length != 5) return "a linha deve ter 5 campos";
        for (int c = 0; c < campos.length; c++) campos[c] = campos[c].trim();

        int numero = converterInteiro(campos[0]);
        if (numero < 1) return "número inválido: \"" + campos[0] + "\"";
        int indice = buscarIndiceProduto(converterInteiro(campos[1]));
        if (indice == -1) return "produto inexistente: \"" + campos[1] + "\"";
        int tipo = converterInteiro(campos[2]);
        if (tipo < 1 || tipo > 3) return "tipo inválido: \"" + campos[2] + "\"";
        int quantidade = converterInteiro(campos[3]);
        if (quantidade < 1 || quantidade > 9999) return "quantidade inválida: \"" + campos[3] + "\"";
        int saldo = converterInteiro(campos[4]);
        if (saldo < 0) return "saldo inválido: \"" + campos[4] + "\"";

        movCodigoProduto[qtdMovimentacoes] = codigos[indice];
        movTipo[qtdMovimentacoes] = tipo;
        movQuantidade[qtdMovimentacoes] = quantidade;
        movSaldoApos[qtdMovimentacoes] = saldo;
        qtdMovimentacoes++;
        totaisMovimentados[indice][tipo - 1] += quantidade; // RN18: remonta a matriz
        return "";
    }

    /** RN15 e RN18: para cada produto, quantidade deve ser = entradas - vendas - perdas. */
    static int verificarConsistencia() {
        int problemas = 0;
        for (int i = 0; i < qtdProdutos; i++) {
            int esperado = totaisMovimentados[i][0] - totaisMovimentados[i][1] - totaisMovimentados[i][2];
            if (quantidades[i] != esperado) { // "!=" = diferente
                System.out.println("INCONSISTÊNCIA: produto " + codigos[i] + " tem quantidade " + quantidades[i]
                        + " em " + ARQ_PRODUTOS + ", mas o histórico indica " + esperado
                        + ". Vale a quantidade do arquivo.");
                problemas++;
            }
        }
        return problemas;
    }

    // ==================================================================
    // GRAVAÇÃO DOS ARQUIVOS (RF16)
    // ==================================================================

    /** RF16: reescreve produtos.txt inteiro. Devolve true se conseguiu. */
    static boolean salvarProdutos() {
        if (!podeGravarProdutos) {
            System.out.println("ATENÇÃO: dados de produtos NÃO gravados (o arquivo não pôde ser lido).");
            return false;
        }
        try (BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ARQ_PRODUTOS, false), StandardCharsets.UTF_8))) {
            saida.write(CABECALHO_PRODUTOS);
            saida.newLine();
            for (int i = 0; i < qtdProdutos; i++) {
                // Junta os campos com ";". O double é gravado com ponto (ex.: 45.9).
                saida.write(codigos[i] + ";" + descricoes[i] + ";" + categorias[i] + ";" + quantidades[i] + ";"
                        + precos[i] + ";" + estoquesMinimos[i] + ";" + ativos[i]);
                saida.newLine();
            }
        } catch (IOException e) { // VG13
            System.out.println("ERRO ao gravar " + ARQ_PRODUTOS + ": " + e.getMessage()
                    + ". Os dados NÃO foram salvos.");
            return false;
        }
        return true;
    }

    /** RF16: acrescenta a movimentação m no FINAL do histórico (modo append). */
    static boolean anexarMovimentacao(int m) {
        if (!podeGravarProdutos) return false; // se os produtos não foram lidos, não mexe no histórico
        File arquivo = new File(ARQ_MOVIMENTACOES);
        boolean novo = !arquivo.exists() || arquivo.length() == 0; // novo ou vazio precisa de cabeçalho
        try {
            boolean faltaQuebra = !novo && !terminaComQuebraDeLinha(arquivo);
            // "true" = append: escreve no final sem apagar o que já existe.
            try (BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(arquivo, true), StandardCharsets.UTF_8))) {
                if (novo) {
                    saida.write(CABECALHO_MOVIMENTACOES);
                    saida.newLine();
                }
                if (faltaQuebra) saida.newLine(); // evita grudar na última linha
                saida.write((m + 1) + ";" + movCodigoProduto[m] + ";" + movTipo[m] + ";"
                        + movQuantidade[m] + ";" + movSaldoApos[m]);
                saida.newLine();
            }
        } catch (IOException e) {
            System.out.println("ERRO ao gravar " + ARQ_MOVIMENTACOES + ": " + e.getMessage()
                    + ". A movimentação NÃO foi salva no histórico.");
            return false;
        }
        return true;
    }

    /** Olha o último caractere do arquivo para ver se ele é um "Enter". */
    static boolean terminaComQuebraDeLinha(File arquivo) throws IOException {
        try (RandomAccessFile leitor = new RandomAccessFile(arquivo, "r")) { // "r" = só leitura
            leitor.seek(arquivo.length() - 1); // vai até a última posição
            return leitor.read() == '\n';
        }
    }
}       