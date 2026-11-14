package minastex_pedidos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class InicioScreenTest {
    @Test void showsOnlyTheThreeNewestOrders() {
        var first = order(1);
        var second = order(2);
        var third = order(3);
        var fourth = order(4);

        assertEquals(List.of(fourth, third, second),
                InicioScreen.latestOrders(List.of(second, fourth, first, third)));
    }

    private static Store.Order order(int number) {
        var order = new Store.Order();
        order.number = number;
        return order;
    }
}
