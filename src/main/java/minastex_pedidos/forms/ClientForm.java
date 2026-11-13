package minastex_pedidos.forms;

import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import megalodonte.components.Select;
import minastex_pedidos.Components;
import minastex_pedidos.Store;
import minastex_pedidos.data.Data;

public final class ClientForm {
        public final State<String> name, document, phone, email, notes, formaPagamento, prazo, frete;
        public ClientForm(Store.Client c) {
            name = State.of(c.name); document = State.of(c.document); phone = State.of(c.phone);
            email = State.of(c.email); notes = State.of(c.notes);
            formaPagamento = State.of(c.formaPagamento);
            prazo = State.of(c.prazo);
            frete = State.of(c.frete);
        }
        public Component render() { return Components.form("Cliente",
                Components.field("Nome/razão social", name),
                Components.field("CPF/CNPJ", document, minastex_pedidos.data.InputType.CPF_CNPJ),
                Components.field("Telefone", phone, minastex_pedidos.data.InputType.PHONE),
                Components.field("E-mail", email),
                Components.field("Observações", notes),
                Components.select("Formas de pagamento", Data.formasPgt, formaPagamento),
                Components.field("Prazo", prazo),
                Components.field("Frete", frete)
                );
        }
    }
