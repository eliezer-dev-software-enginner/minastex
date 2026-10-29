package minastex_pedidos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PedidoViewModelTest {
    @TempDir Path temp;

    @Test void recalculatesWhenMaterialPriceAndDimensionsChange() throws Exception {
        AppController app = new AppController(new Store(temp.resolve("orders.json")));
        PedidoViewModel order = new PedidoViewModel(app, null);
        order.addGroup();
        PedidoViewModel.GroupViewModel group = order.groups.get().getFirst();
        assertEquals(1, group.items.get().size());
        group.material.set(app.store.data.materials.getFirst());
        PedidoViewModel.ItemViewModel item = group.items.get().getFirst();
        item.length.set("2"); item.width.set("1"); item.quantity.set("3");
        assertEquals(0, new java.math.BigDecimal("152.94").compareTo(Store.subtotal(order.draft)));
        assertTrue(order.totals.get().contains("152,94"));
        group.price.set("10");
        assertEquals(0, new java.math.BigDecimal("60.00").compareTo(Store.groupValue(group.data)));
        assertTrue(order.totals.get().contains("60,00"));
    }
}
