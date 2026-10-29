package minastex_pedidos;

import megalodonte.base.state.State;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Editable order data and calculations, independent of its component tree. */
final class PedidoViewModel {
    final State<Store.Client> client;
    final State<LocalDate> date;
    final State<String> ipi;
    final State<String> weight;
    final State<String> freight;
    final State<String> payment;
    final State<String> paymentTerm;
    final State<String> pickupDate;
    final State<String> notes;
    final State<Boolean> includeBank;
    final State<Store.Account> account;
    final State<String> bankText;
    final State<String> totals = State.of("");
    final State<List<GroupViewModel>> groups;
    final Store.Order draft;
    final Store.Order original;
    final AppController app;

    PedidoViewModel(AppController app, Store.Order original) {
        this.app = app; this.original = original;
        draft = original == null ? new Store.Order() : Store.copy(original);
        if (draft.number == 0) draft.number = app.store.nextNumber();
        if (original == null) draft.ipiPercent = app.store.data.company.ipiPercent;
        client = State.of(app.store.data.clients.stream().filter(c -> c.id.equals(draft.clientId)).findFirst().orElse(null));
        date = State.of(LocalDate.parse(draft.date)); ipi = State.of(draft.ipiPercent.toPlainString());
        weight = State.of(draft.weight); freight = State.of(draft.freight); payment = State.of(draft.payment);
        paymentTerm = State.of(draft.paymentTerm); pickupDate = State.of(draft.pickupDate);
        notes = State.of(draft.notes); includeBank = State.of(draft.includeBank);
        account = State.of(null); bankText = State.of(draft.bankText);
        groups = State.of(draft.groups.stream().map(group -> new GroupViewModel(this, group)).toList());
        account.subscribe(value -> {
            if (value != null) bankText.set(value.bank + " | Agência " + value.agency + " | Conta " + value.number
                    + " | " + value.beneficiary + " | " + value.document + " | PIX " + value.pix);
        });
        ipi.subscribe(ignored -> recalculate());
    }

    void addGroup() {
        Store.Group group = new Store.Group();
        group.items.add(new Store.Item());
        draft.groups.add(group);
        List<GroupViewModel> updated = new ArrayList<>(groups.get());
        updated.add(new GroupViewModel(this, group)); groups.set(List.copyOf(updated)); recalculate();
    }

    void removeGroup(GroupViewModel group) {
        if (!AppController.confirm("Remover grupo?")) return;
        draft.groups.remove(group.data);
        List<GroupViewModel> updated = new ArrayList<>(groups.get());
        updated.remove(group); groups.set(List.copyOf(updated)); recalculate();
    }

    void recalculate() {
        try { draft.ipiPercent = Store.decimal(ipi.get()); }
        catch (Exception error) { draft.ipiPercent = BigDecimal.valueOf(-1); }
        totals.set("Subtotal " + Money.money(Store.subtotal(draft)) + "   IPI " + Money.money(Store.ipi(draft))
                + "   TOTAL " + Money.money(Store.total(draft)));
    }

    private void capture() {
        draft.date = date.get() == null ? "" : date.get().toString();
        Store.Client selected = client.get();
        if (selected != null) {
            draft.clientId = selected.id; draft.clientName = selected.name; draft.clientDocument = selected.document;
        }
        draft.weight = weight.get().trim(); draft.freight = freight.get().trim();
        draft.payment = payment.get().trim(); draft.paymentTerm = paymentTerm.get().trim();
        draft.pickupDate = pickupDate.get().trim(); draft.notes = notes.get().trim();
        draft.includeBank = includeBank.get(); draft.bankText = bankText.get().trim();
    }

    private boolean valid() {
        if (draft.clientName.isBlank() || draft.date.isBlank()) { AppController.alert("Selecione cliente e data."); return false; }
        if (draft.ipiPercent.signum() < 0) { AppController.alert("IPI inválido."); return false; }
        if (draft.groups.isEmpty()) { AppController.alert("Adicione um grupo e um item."); return false; }
        for (Store.Group group : draft.groups) {
            if (group.materialName.isBlank() || group.price.signum() < 0 || group.items.isEmpty()) {
                AppController.alert("Cada grupo precisa de acabamento, preço e itens válidos."); return false;
            }
            for (Store.Item item : group.items) if (item.length.signum() <= 0 || item.width.signum() <= 0 || item.quantity <= 0) {
                AppController.alert("Comprimento, largura e quantidade devem ser positivos."); return false;
            }
        }
        return true;
    }

    void save() {
        capture(); if (!valid()) return;
        int index = app.store.data.orders.indexOf(original);
        if (index < 0) app.store.data.orders.add(draft);
        else app.store.data.orders.set(index, draft);
        if (app.save()) app.history();
    }

    void preview() { capture(); if (valid()) app.preview(draft); }
    void export() { capture(); if (valid()) app.export(draft); }

    static final class GroupViewModel {
        final State<Store.Material> material;
        final State<String> price;
        final State<String> subtotal = State.of("");
        final State<List<ItemViewModel>> items;
        final Store.Group data;
        final PedidoViewModel order;

        GroupViewModel(PedidoViewModel order, Store.Group data) {
            this.order = order; this.data = data;
            material = State.of(order.app.store.data.materials.stream()
                    .filter(m -> m.id.equals(data.materialId)).findFirst().orElse(null));
            price = State.of(data.price.toPlainString());
            items = State.of(data.items.stream().map(item -> new ItemViewModel(this, item)).toList());
            material.subscribe(selected -> {
                if (selected != null && !selected.id.equals(data.materialId)) {
                    data.materialId = selected.id; data.materialName = selected.name;
                    price.set(selected.price.toPlainString());
                }
            });
            price.subscribe(value -> {
                try { data.price = Store.decimal(value); }
                catch (Exception error) { data.price = BigDecimal.valueOf(-1); }
                for (ItemViewModel item : items.get()) item.recalculate();
                recalculate();
            });
        }

        void addItem() {
            Store.Item item = new Store.Item(); data.items.add(item);
            List<ItemViewModel> updated = new ArrayList<>(items.get());
            updated.add(new ItemViewModel(this, item)); items.set(List.copyOf(updated)); recalculate();
        }

        void removeItem(ItemViewModel item) {
            if (!AppController.confirm("Excluir linha?")) return;
            data.items.remove(item.data);
            List<ItemViewModel> updated = new ArrayList<>(items.get());
            updated.remove(item); items.set(List.copyOf(updated)); recalculate();
        }

        void recalculate() {
            subtotal.set("Grupo: " + Money.area(Store.groupArea(data)) + " · " + Money.money(Store.groupValue(data)));
            order.recalculate();
        }
    }

    static final class ItemViewModel {
        final State<String> length;
        final State<String> width;
        final State<String> quantity;
        final State<String> area = State.of("");
        final State<String> value = State.of("");
        final Store.Item data;
        final GroupViewModel group;

        ItemViewModel(GroupViewModel group, Store.Item data) {
            this.group = group; this.data = data;
            length = State.of(data.length.toPlainString()); width = State.of(data.width.toPlainString());
            quantity = State.of(Integer.toString(data.quantity));
            length.subscribe(ignored -> recalculate()); width.subscribe(ignored -> recalculate());
            quantity.subscribe(ignored -> recalculate());
        }

        void recalculate() {
            try { data.length = Store.decimal(length.get()); } catch (Exception error) { data.length = BigDecimal.ZERO; }
            try { data.width = Store.decimal(width.get()); } catch (Exception error) { data.width = BigDecimal.ZERO; }
            try { data.quantity = Integer.parseInt(quantity.get()); } catch (Exception error) { data.quantity = 0; }
            area.set(Money.area(Store.area(data)));
            value.set(Money.money(Store.itemValue(data, group.data)));
            group.recalculate();
        }
    }
}
