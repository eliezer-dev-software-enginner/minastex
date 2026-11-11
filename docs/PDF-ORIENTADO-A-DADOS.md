# PDF orientado a dados

O pedido comercial usa o motor reutilizável `pack.utilities.pdf.DataPdf`, mantido
em `pack-utilities`. Essa é a consolidação como biblioteca do gerador orientado a
dados iniciado no Plics SW. O Gobitech Pesagem ainda possui exportadores específicos
e pode ser migrado posteriormente, sem vincular a biblioteca ao domínio de pesagem.

## Responsabilidades

- `src/main/resources/pdf/order.jsonc` define página, fontes, cabeçalho, logomarca,
  textos, grupos, tabela, condições e espaçamentos.
- `OrderPdf` transforma `Store.Order` e `Store.Company` em mapas com valores já
  calculados e formatados. Ele não conhece PDFBox nem coordenadas de desenho.
- `DataPdf` interpreta o JSONC, resolve expressões `${campo}`, pagina tabelas,
  repete o cabeçalho e grava o documento.
- `AppController` usa o mesmo caminho para prévia e exportação.

O template é a fonte de verdade do layout. Mudanças de apresentação devem ser feitas
nele; cálculos comerciais e formatações do domínio permanecem no aplicativo. A API do
motor não recebe classes Minastex, o que permite reutilizá-la em outros projetos.

## Validação

`OrderTest` gera um pedido de várias páginas e verifica título repetido, dados da
empresa, CNPJ formatado, cliente e dados bancários. O artefato de diagnóstico fica em
`build/reports/pdf-preview/order.pdf` e não é versionado.

`DataPdfTest`, em `pack-utilities`, testa o motor isoladamente com JSONC, repetição,
condição, tabela e paginação. Ao alterar o motor, execute e publique a biblioteca antes
de validar o aplicativo:

```sh
cd java-world/utilities-pack
./gradlew test publishToMavenLocal

cd ../../megalodonte-world/applications/minastex-pedidos
./gradlew test --tests minastex_pedidos.OrderTest --rerun-tasks
```
