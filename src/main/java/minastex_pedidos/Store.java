package minastex_pedidos;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Local, atomic JSON storage. Orders keep commercial snapshots independent of registries. */
public final class Store {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path file;
    public Data data;

    public Store(Path file) throws IOException {
        this.file = file;
        boolean existing = Files.isRegularFile(file);
        data = existing ? JSON.readValue(file.toFile(), Data.class) : new Data();
        if (data.clients == null) data.clients = new ArrayList<>();
        if (data.materials == null) data.materials = new ArrayList<>();
        if (data.accounts == null) data.accounts = new ArrayList<>();
        if (data.orders == null) data.orders = new ArrayList<>();
        if (data.company == null) data.company = new Company();
        if (data.company.phone == null || data.company.phone.isBlank()) data.company.phone = Company.DEFAULT_PHONE;
        if (data.company.email == null || data.company.email.isBlank()) data.company.email = Company.DEFAULT_EMAIL;
        if (data.company.website == null || data.company.website.isBlank()) data.company.website = Company.DEFAULT_WEBSITE;
        if (!existing) {
            seed("Branco 2 Faces", "25.49");
            seed("Preto Opaline 2 Faces", "30.61");
            seed("Preto Opaline 1 Face", "23.51");
            seed("Branco Gelo MTX", "12.94");
            save();
        }
    }

    private void seed(String name, String price) {
        Material material = new Material(); material.name = name; material.price = new BigDecimal(price);
        data.materials.add(material);
    }

    public static Path defaultFile() {
        String override = System.getProperty("minastex.data");
        return override == null || override.isBlank()
                ? Path.of(System.getProperty("user.home"), ".local/share/minastex-pedidos/data.json")
                : Path.of(override).toAbsolutePath().normalize();
    }

    public synchronized void save() throws IOException {
        data.nextOrderNumber = Math.max(data.nextOrderNumber,
                data.orders.stream().mapToInt(order -> order.number).max().orElse(0) + 1);
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), ".minastex-", ".tmp");
        try {
            JSON.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), data);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }

    public int nextNumber() { return Math.max(data.nextOrderNumber,
            data.orders.stream().mapToInt(o -> o.number).max().orElse(0) + 1); }
    public static String id() { return UUID.randomUUID().toString(); }

    public static BigDecimal amount(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    public static BigDecimal decimal(String raw) {
        String text = raw == null ? "" : raw.trim().replace(" ", "");
        if (text.contains(",")) text = text.replace(".", "").replace(',', '.');
        return new BigDecimal(text);
    }

    public static BigDecimal area(Item item) {
        return item.length.multiply(item.width).multiply(BigDecimal.valueOf(item.quantity));
    }
    public static BigDecimal itemValue(Item item, Group group) {
        return amount(area(item).multiply(group.price));
    }
    public static BigDecimal groupArea(Group group) {
        return group.items.stream().map(Store::area).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public static BigDecimal groupValue(Group group) {
        return group.items.stream().map(item -> itemValue(item, group)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public static BigDecimal subtotal(Order order) {
        return order.groups.stream().map(Store::groupValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public static BigDecimal ipi(Order order) {
        return amount(subtotal(order).multiply(order.ipiPercent).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
    }
    public static BigDecimal total(Order order) { return subtotal(order).add(ipi(order)); }
    public static Order copy(Order order) { return JSON.convertValue(order, Order.class); }
    public static Company copy(Company company) { return JSON.convertValue(company, Company.class); }

    public static final class Data {
        public int nextOrderNumber = 1;
        public List<Client> clients = new ArrayList<>();
        public List<Material> materials = new ArrayList<>();
        public List<Account> accounts = new ArrayList<>();
        public List<Order> orders = new ArrayList<>();
        public Company company = new Company();
    }
    public static final class Client {
        public String id = Store.id(), name = "", document = "", phone = "", email = "", notes = "";
        public boolean active = true;
        @Override public String toString() { return name; }
    }
    public static final class Material {
        public String id = Store.id(), name = "";
        public BigDecimal price = BigDecimal.ZERO;
        public boolean active = true;
        @Override public String toString() { return name + " · " + Money.money(price) + "/m²"; }
    }
    public static final class Account {
        public String id = Store.id(), label = "", bank = "", agency = "", number = "", beneficiary = "", document = "", pix = "";
        @Override public String toString() { return label; }
    }
    public static final class Company {
        public static final String DEFAULT_PHONE = "(32) 3532-4583 / (32) 98405-3638";
        public static final String DEFAULT_EMAIL = "minastex@minastex.com.br";
        public static final String DEFAULT_WEBSITE = "www.minastex.com.br";
        public String name = "Minastex", legalName = "", document = "", phone = DEFAULT_PHONE,
                email = DEFAULT_EMAIL, website = DEFAULT_WEBSITE, address = "", logo = "";
        public BigDecimal ipiPercent = new BigDecimal("3.25");
    }
    public static final class Item {
        public String id = Store.id();
        public BigDecimal length = BigDecimal.ZERO, width = BigDecimal.ZERO;
        public int quantity;
    }
    public static final class Group {
        public String id = Store.id(), materialId = "", materialName = "";
        public BigDecimal price = BigDecimal.ZERO;
        public List<Item> items = new ArrayList<>();
    }
    public static final class Order {
        public String id = Store.id(), clientId = "", clientName = "", clientDocument = "", date = LocalDate.now().toString();
        public int number;
        public BigDecimal ipiPercent = new BigDecimal("3.25");
        public String weight = "", freight = "", payment = "", paymentTerm = "", pickupDate = "", notes = "";
        public boolean includeBank;
        public String bankText = "";
        public List<Group> groups = new ArrayList<>();
        @Override public String toString() { return "Pedido " + number + " · " + clientName; }
    }
}
