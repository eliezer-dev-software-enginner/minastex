# PDF orientado a dados

O pedido comercial usa o motor reutilizável `pack.utilities.pdf.DataPdf`, mantido
em `pack-utilities`. Essa é a consolidação como biblioteca do gerador orientado a
dados iniciado no Plics SW. O Gobitech Pesagem ainda possui exportadores específicos
e pode ser migrado posteriormente, sem vincular a biblioteca ao domínio de pesagem.

## Responsabilidades

- `src/main/resources/pdf/order.jsonc` define página, fontes, cabeçalho, logomarca,
  faixas de seção, colunas, tabela, condições comerciais, cores e espaçamentos.
- `src/main/resources/assets/minastex-logo-pdf.png` é a variante recortada da
  identidade oficial usada no cabeçalho, sem as margens brancas do arquivo original.
- `OrderPdf` transforma `Store.Order` e `Store.Company` em mapas com valores já
  calculados e formatados. Ele não conhece PDFBox nem coordenadas de desenho.
- `DataPdf` interpreta o JSONC, resolve expressões `${campo}`, compõe faixas e
  colunas, estiliza e pagina tabelas, repete o cabeçalho e grava o documento.
- `AppController` usa o mesmo caminho para prévia e exportação.

O template é a fonte de verdade do layout. Mudanças de apresentação devem ser feitas
nele; cálculos comerciais e formatações do domínio permanecem no aplicativo. A API do
motor não recebe classes Minastex, o que permite reutilizá-la em outros projetos.

## Identidade do pedido

O layout segue a referência comercial real fornecida pela Minastex: dados
cadastrais à esquerda e, na coluna direita, logomarca, `PEDIDO`, número e data
empilhados nessa ordem; abaixo ficam blocos azuis para cliente, itens e condições
comerciais, detalhes verdes e total destacado. Os itens de grupos de
acabamento são achatados em uma única tabela com descrição, dimensões lineares em
metros, quantidade, metragem total em metros quadrados, preço por metro quadrado e
total. A coluna `PREÇO/M²` preserva o preço comercial cadastrado no grupo; ela não
é o valor calculado de uma peça. A metragem de cada linha é
`comprimento × largura × quantidade`. O cabeçalho
também apresenta razão social/CNPJ, endereço, telefone e e-mail cadastrados para a
Minastex; valores ausentes são omitidos.

O resumo financeiro reserva 50% da largura útil para subtotal, IPI e total, de
modo que valores usuais mantenham a fonte de 10 pt. A faixa `TOTAL DO PEDIDO`
também usa `fitMode: "SHRINK"` como proteção para valores excepcionalmente longos:
ela preserva o texto completo e nunca o substitui por reticências.

Nome, CPF/CNPJ, telefone, e-mail e endereço do cliente são copiados para o pedido
quando ele é salvo. Esse snapshot evita que uma edição futura do cadastro altere um documento
comercial histórico. Pedidos antigos continuam compatíveis e simplesmente omitem
os campos de contato que ainda não existiam.

Documento, telefone, e-mail, endereço, observações, forma de pagamento, frete e
prazo são blocos opcionais: o PDF não renderiza o respectivo rótulo nem reserva
sua área quando o valor está vazio.

## Validação

`OrderTest` gera um pedido comercial de uma página para revisão visual e outro de
várias páginas. A regressão do total reproduz os dados do pedido Beatriz fornecido
em 24/11/2026: cinco medidas, quantidades, preço de R$ 13,85/m², subtotal de
R$ 167.798,85, IPI de R$ 5.453,46 e total de R$ 173.252,31. O teste também verifica
cabeçalho repetido, data extensa em português, contatos formatados, ausência integral dos blocos opcionais
vazios, um cenário com exatamente 20 itens, seções comerciais, paginação e dados
bancários opcionais. Os artefatos
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
