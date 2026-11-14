# Anúncios institucionais

O Minastex Pedidos apresenta uma vitrine discreta de soluções EliezerDev com
dois níveis:

- o painel inicial mostra no máximo dois destaques selecionados para a empresa;
- **Soluções EliezerDev**, na barra lateral, mostra todas as campanhas destinadas
  ao aplicativo.

Os dados vêm de `https://eliezer-dev.vercel.app/anuncios`. O aplicativo lê apenas
marcação HTML declarada com `data-minastex-ad="true"`; títulos, descrições,
imagens e links são tratados como dados, sem executar scripts da página. Enquanto
o feed específico ainda não estiver publicado, as imagens das campanhas antigas
podem ser usadas como compatibilidade temporária, mas recebem títulos e descrições
contextualizados para a Minastex dentro do aplicativo.

A consulta roda em uma virtual thread, tem limites de conexão, resposta e
tamanho, e falha silenciosamente com registro em nível `debug`. Assim, ausência
de internet, imagem inválida ou indisponibilidade do portfólio nunca impede o
cadastro e a consulta de pedidos. Imagens usam `AsyncImage`, que carrega
em segundo plano para não bloquear a thread da interface.

As campanhas ficam fora do binário para que textos, destinos e destaques possam
ser atualizados sem republicar o aplicativo. O conteúdo deve permanecer
institucional e contextual à Minastex: automação de novos processos, integrações,
bots e outros sistemas sob medida — não publicidade genérica de terceiros.
