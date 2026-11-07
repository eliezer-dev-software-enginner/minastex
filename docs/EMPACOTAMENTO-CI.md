# Empacotamento do Minastex no GitHub Actions

O [workflow do projeto](../.github/workflows/package.yml) chama o workflow
compartilhado de `megalodonte-packaging` e informa `dependencies: 'disgust'`.
Esse valor é um nome reconhecido pelo preparador compartilhado, não uma
coordenada Maven nem uma URL. Ele corresponde ao repositório
[`disgust-components`](https://github.com/eliezer-dev-software-enginner/disgust-components).

Antes de compilar o Minastex, o preparador clona e publica as bibliotecas do
framework; ao selecionar `disgust`, ele também prepara `pack-utilities`, clona
`disgust-components` e publica tanto `distust-io-components` quanto
`disgust-icons` no Maven Local do runner. O Gradle do projeto então encontra
`disgust:distust-io-components:1.0.0-beta`. O alias atual e as demais opções
estão documentados no [guia do workflow compartilhado](https://github.com/eliezer-dev-software-enginner/megalodonte-packaging/blob/main/docs/GITHUB_ACTIONS.md).

Se o job não encontrar esse artefato, confira primeiro o valor de `dependencies`
no workflow e, no log, a etapa **Build and publish dependencies to Maven local**.
O teste local da seleção de dependências não substitui uma execução no runner
Windows ou Linux do GitHub.

Aceitar URLs arbitrárias de repositórios Gradle é uma possibilidade para o futuro
do empacotador compartilhado; não é suportado pelo campo `dependencies` atual.
