# PDF orientado a dados

O pedido comercial usa o motor reutilizável `pack.utilities.pdf.DataPdf`, mantido
em `pack-utilities`. Essa é a consolidação como biblioteca do gerador orientado a
dados iniciado no Plics SW. O Gobitech Pesagem ainda possui exportadores específicos
e pode ser migrado posteriormente, sem vincular a biblioteca ao domínio de pesagem.

## Responsabilidades

- `src/main/resources/pdf/order.jsonc` define página, fontes, cabeçalho, logomarca,
  faixas de seção, colunas, tabela, condições comerciais, cores e espaçamentos.
- `OrderPdf` transforma `Store.Order` e `Store.Company` em mapas com valores já
  calculados e formatados. Ele não conhece PDFBox nem coordenadas de desenho.
- `DataPdf` interpreta o JSONC, resolve expressões `${campo}`, compõe faixas e
  colunas, estiliza e pagina tabelas, repete o cabeçalho e grava o documento.
- `AppController` usa o mesmo caminho para prévia e exportação.

O template é a fonte de verdade do layout. Mudanças de apresentação devem ser feitas
nele; cálculos comerciais e formatações do domínio permanecem no aplicativo. A API do
motor não recebe classes Minastex, o que permite reutilizá-la em outros projetos.

## Identidade do pedido

O layout segue a referência comercial real fornecida pela Minastex: logomarca à
esquerda, `PEDIDO`, número e data à direita, blocos azuis para cliente, itens e
condições comerciais, detalhes amarelos e total destacado. Os itens de grupos de
acabamento são achatados em uma única tabela com descrição e medidas, quantidade,
preço unitário da peça e total.

Nome, CPF/CNPJ, telefone, e-mail e endereço do cliente são copiados para o pedido
quando ele é salvo. Esse snapshot evita que uma edição futura do cadastro altere um documento
comercial histórico. Pedidos antigos continuam compatíveis e exibem em branco os
campos de contato que ainda não existiam.

## Validação

`OrderTest` gera um pedido comercial de uma página para revisão visual e outro de
várias páginas. Ele verifica cabeçalho repetido, data extensa em português, contatos
formatados, seções comerciais, paginação e dados bancários opcionais. Os artefatos
de diagnóstico ficam em `build/reports/pdf-preview/` e não são versionados.

`DataPdfTest`, em `pack-utilities`, testa o motor isoladamente com JSONC, repetição,
condição, faixas coloridas, colunas, tabela estilizada e paginação. Ao alterar o
motor, execute e publique a biblioteca antes de validar o aplicativo:

```sh
cd java-world/utilities-pack
./gradlew test publishToMavenLocal

cd ../../megalodonte-world/applications/minastex-pedidos
./gradlew test --tests minastex_pedidos.OrderTest --rerun-tasks
```
