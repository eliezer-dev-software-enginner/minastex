package minastex_pedidos.forms;

import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import minastex_pedidos.Components;
import minastex_pedidos.Store;

public final class AccountForm {
    public final State<String> label;
    public final State<String> bank;
    public final State<String> agency;
    public final State<String> number;
    public final State<String> beneficiary;
    public final State<String> document;
    public final State<String> pix;

    public AccountForm(Store.Account a) {
        label = State.of(a.label); bank = State.of(a.bank); agency = State.of(a.agency);
        number = State.of(a.number); beneficiary = State.of(a.beneficiary);
        document = State.of(a.document); pix = State.of(a.pix);
    }
    public Component render() { return Components.form("Conta bancária", Components.field("Identificação", label),
            Components.field("Banco", bank), Components.field("Agência", agency),
            Components.field("Conta", number), Components.field("Favorecido", beneficiary),
            Components.field("CPF/CNPJ", document, minastex_pedidos.data.InputType.CPF_CNPJ), Components.field("Chave PIX", pix)); }
}