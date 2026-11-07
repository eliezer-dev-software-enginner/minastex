package minastex_pedidos.forms;

import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import minastex_pedidos.Components;
import minastex_pedidos.Store;

public final class MaterialForm {
        public final State<String> name, price;
        public MaterialForm(Store.Material m) { name = State.of(m.name); price = State.of(m.price.toPlainString()); }
        public Component render() { return Components.form("Acabamento", Components.field("Nome", name),
                Components.field("Preço por m²", price, minastex_pedidos.data.InputType.DECIMAL_FIXED_TWO)); }
    }