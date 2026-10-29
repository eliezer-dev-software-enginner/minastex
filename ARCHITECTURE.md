# Arquitetura de interface

O contrato para a interface desktop está em [megalodonte-native-ui](../../.agents/skills/megalodonte-native-ui/SKILL.md).

`HomeScreen` é apenas o shell declarativo da navegação. Cada destino do menu tem um `ScreenComponent`: `InicioScreen`, `PedidoScreen`, `HistoricoScreen`, `CadastroScreen` ou `EmpresaScreen`. `AppController` concentra navegação, persistência e exportação. `PedidoViewModel` mantém os estados e os cálculos do editor; os formulários de cadastro têm estados próprios. `Components` contém as fábricas estáticas da linguagem visual compartilhada.

`StateView` em `megalodonte-components` apresenta a tela atual a partir de `State`. `StateList` mantém os componentes dos grupos e itens existentes ao atualizar as coleções, preservando o texto em edição. Nenhuma tela manipula filhos ou acessa o nó JavaFX. A seleção de arquivos e os diálogos são APIs da biblioteca. Estilos comuns usam `Props` e tokens do tema. O modelo de dados e a geração de PDF continuam independentes da UI.
