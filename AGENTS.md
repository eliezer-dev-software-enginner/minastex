# Interface Minastex

Antes de cada trabalho de UI, execute `megalodonte skills sync --project .` se o CLI estiver disponível e siga `skills/megalodonte-native-ui/SKILL.md`. Esta aplicação deve usar composição declarativa Megalodonte/Disgust, estados no topo da tela ou em ViewModel e fábricas estáticas em `Components`. Não use `getJavaFxNode()`, `getChildren().add(...)`, estilos inline nem campos de componentes; para uma referência imperativa excepcional, use `Ref<T>`.
