package minastex_pedidos;

import disgust.icons.Feather;
import disgust.io.ButtonsPack;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Megalodonte screen: Disgust buttons, local registries, order editor and PDF export. */
public final class HomeScreen implements ScreenComponent {
    private final Store store;
    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();

    public HomeScreen() {
        try { store = new Store(Store.defaultFile()); }
        catch (Exception error) { throw new IllegalStateException("Não foi possível abrir os pedidos", error); }
    }

    @Override public Component render() {
        root.setStyle("-fx-background-color:#f3f6f9;-fx-font-family:'DejaVu Sans';");
        VBox side = new VBox(8);
        side.setPadding(new Insets(22,12,22,12));
        side.setPrefWidth(220);
        side.setStyle("-fx-background-color:#172b3a;");
        Label brand = new Label("MINASTEX\nPEDIDOS");
        brand.setStyle("-fx-text-fill:white;-fx-font-size:20px;-fx-font-weight:bold;");
        side.getChildren().addAll(brand, new Separator(),
                nav("Início", Feather.HOME, this::home),
                nav("Novo pedido", Feather.FILE_PLUS, () -> order(null)),
                nav("Histórico", Feather.LIST, this::history),
                nav("Clientes", Feather.USERS, () -> registry("Clientes")),
                nav("Acabamentos", Feather.LAYERS, () -> registry("Acabamentos")),
                nav("Contas bancárias", Feather.CREDIT_CARD, () -> registry("Contas bancárias")),
                nav("Empresa", Feather.SETTINGS, this::company));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Hyperlink credit = new Hyperlink("Desenvolvido por EliezerDev");
        credit.setStyle("-fx-text-fill:#a9dceb;-fx-font-size:12px;-fx-border-color:transparent;-fx-padding:4px 0;");
        credit.setOnAction(event -> {
            try { Desktop.getDesktop().browse(URI.create("https://eliezer-dev.vercel.app/")); }
            catch (Exception error) { alert("Não foi possível abrir o portfólio: " + error.getMessage()); }
        });
        side.getChildren().addAll(spacer, credit);
        root.setLeft(side); root.setCenter(content); home();
        return Component.CreateFromJavaFxNode(root);
    }

    private Node nav(String title, disgust.icons.Icon icon, Runnable run) {
        var button = (Button) ButtonsPack.TextButton(title, new ButtonsPack.Props().iconStart(icon).onClick(run::run)).getJavaFxNode();
        button.setMaxWidth(Double.MAX_VALUE); button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("-fx-text-fill:#e7f4f7;-fx-background-color:transparent;-fx-padding:11px;");
        return button;
    }
    private Button button(String title, Runnable run) {
        return (Button) ButtonsPack.ContainedButton(title, new ButtonsPack.Props().onClick(run::run)).getJavaFxNode();
    }
    private VBox page(String title) {
        VBox box = new VBox(14); box.setPadding(new Insets(26));
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#15394b;");
        box.getChildren().add(heading);
        ScrollPane scroll = new ScrollPane(box); scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:#f3f6f9;-fx-background-color:transparent;");
        content.getChildren().setAll(scroll); return box;
    }
    private void home() {
        VBox box = page("Painel de pedidos");
        ImageView logo = new ImageView(new Image(HomeScreen.class.getResourceAsStream("/assets/minastex-logo.png")));
        logo.setFitWidth(230);
        logo.setPreserveRatio(true);
        box.getChildren().addAll(logo, new Label("Pedidos comerciais com cálculos automáticos e PDF."),
                row(button("+ Novo pedido", () -> order(null)), button("Histórico", this::history)),
                new Label(store.data.orders.size() + " pedidos cadastrados"));
        store.data.orders.stream().sorted((a,b) -> Integer.compare(b.number,a.number)).limit(5).forEach(o ->
                box.getChildren().add(button("Nº " + o.number + " · " + o.clientName + " · " + Money.money(Store.total(o)), () -> order(o))));
    }
    private void registry(String kind) {
        VBox box = page(kind);
        TextField search = field("Pesquisar");
        TableView<Object> table = new TableView<>(); table.setPrefHeight(500);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        if (kind.equals("Clientes")) {
            table.getColumns().addAll(col("Nome", x -> ((Store.Client)x).name), col("CPF/CNPJ", x -> ((Store.Client)x).document),
                    col("Telefone", x -> ((Store.Client)x).phone), col("E-mail", x -> ((Store.Client)x).email),
                    col("Situação", x -> ((Store.Client)x).active ? "Ativo" : "Inativo"));
        } else if (kind.equals("Acabamentos")) {
            table.getColumns().addAll(col("Acabamento", x -> ((Store.Material)x).name),
                    col("Preço/m²", x -> Money.money(((Store.Material)x).price)),
                    col("Situação", x -> ((Store.Material)x).active ? "Ativo" : "Inativo"));
        } else {
            table.getColumns().addAll(col("Identificação", x -> ((Store.Account)x).label),
                    col("Banco", x -> ((Store.Account)x).bank), col("Agência", x -> ((Store.Account)x).agency),
                    col("Conta", x -> ((Store.Account)x).number));
        }
        Runnable refresh = () -> {
            List<?> values = kind.equals("Clientes") ? store.data.clients : kind.equals("Acabamentos") ? store.data.materials : store.data.accounts;
            table.setItems(FXCollections.observableArrayList(values.stream()
                    .filter(x -> x.toString().toLowerCase().contains(search.getText().toLowerCase())).toList()));
        };
        search.textProperty().addListener((o,a,b) -> refresh.run()); refresh.run();
        box.getChildren().addAll(search, row(button("+ Cadastrar", () -> { editRegistry(kind,null); refresh.run(); }),
                button("Editar", () -> { if (table.getSelectionModel().getSelectedItem()!=null) { editRegistry(kind,table.getSelectionModel().getSelectedItem()); refresh.run(); } }),
                button("Ativar/desativar", () -> {
                    Object x=table.getSelectionModel().getSelectedItem();
                    if (x instanceof Store.Client c) c.active=!c.active;
                    else if (x instanceof Store.Material m) m.active=!m.active;
                    else return;
                    persist(); refresh.run();
                }),
                button("Excluir", () -> {
                    Object x=table.getSelectionModel().getSelectedItem(); if (x==null || !confirm("Excluir registro?")) return;
                    if (x instanceof Store.Client c) {
                        if (store.data.orders.stream().anyMatch(o -> o.clientId.equals(c.id))) { alert("Cliente usado em pedido: desative-o."); return; }
                        store.data.clients.remove(c);
                    } else if (x instanceof Store.Material m) {
                        if (store.data.orders.stream().flatMap(o -> o.groups.stream()).anyMatch(g -> g.materialId.equals(m.id))) { alert("Acabamento usado em pedido: desative-o."); return; }
                        store.data.materials.remove(m);
                    } else store.data.accounts.remove(x);
                    persist(); refresh.run();
                })), table);
    }
    private void editRegistry(String kind, Object existing) {
        if (kind.equals("Clientes")) {
            Store.Client c=existing==null?new Store.Client():clone(existing,Store.Client.class);
            TextField name=filled("Nome/razão social",c.name), doc=filled("CPF/CNPJ",c.document),
                    phone=filled("Telefone",c.phone), email=filled("E-mail",c.email), notes=filled("Observações",c.notes);
            if (!form(kind,name,doc,phone,email,notes)) return;
            if(name.getText().isBlank()){alert("Informe o nome.");return;}
            c.name=name.getText().trim(); c.document=doc.getText().trim(); c.phone=phone.getText().trim();
            c.email=email.getText().trim(); c.notes=notes.getText().trim();
            replace(store.data.clients,(Store.Client)existing,c);
        } else if (kind.equals("Acabamentos")) {
            Store.Material m=existing==null?new Store.Material():clone(existing,Store.Material.class);
            TextField name=filled("Nome",m.name), price=filled("Preço por m²",m.price.toPlainString());
            if(!form(kind,name,price))return;
            try { BigDecimal p=Store.decimal(price.getText()); if(name.getText().isBlank()||p.signum()<0)throw new IllegalArgumentException();
                m.name=name.getText().trim();m.price=Store.amount(p);replace(store.data.materials,(Store.Material)existing,m);
            } catch(Exception bad){alert("Informe nome e preço válido.");return;}
        } else {
            Store.Account a=existing==null?new Store.Account():clone(existing,Store.Account.class);
            TextField label=filled("Identificação",a.label), bank=filled("Banco",a.bank), agency=filled("Agência",a.agency),
                    number=filled("Conta",a.number), beneficiary=filled("Favorecido",a.beneficiary),
                    doc=filled("CPF/CNPJ",a.document), pix=filled("Chave PIX",a.pix);
            if(!form(kind,label,bank,agency,number,beneficiary,doc,pix))return;
            if(label.getText().isBlank()||bank.getText().isBlank()){alert("Informe identificação e banco.");return;}
            a.label=label.getText().trim();a.bank=bank.getText().trim();a.agency=agency.getText().trim();
            a.number=number.getText().trim();a.beneficiary=beneficiary.getText().trim();a.document=doc.getText().trim();a.pix=pix.getText().trim();
            replace(store.data.accounts,(Store.Account)existing,a);
        }
        persist();
    }
    private void company() {
        VBox box=page("Configurações da empresa"); Store.Company c=store.data.company;
        TextField name=filled("Nome",c.name), legal=filled("Razão social",c.legalName), doc=filled("CNPJ",c.document),
                phone=filled("Telefones",c.phone), email=filled("E-mail",c.email), website=filled("Site",c.website),
                address=filled("Endereço",c.address),
                logo=filled("Caminho da logomarca",c.logo), ipi=filled("IPI padrão (%)",c.ipiPercent.toPlainString());
        box.getChildren().addAll(name,legal,doc,phone,email,website,address,row(logo,button("Selecionar imagem",()->{
            FileChooser picker=new FileChooser(); picker.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagem","*.png","*.jpg","*.jpeg"));
            var chosen=picker.showOpenDialog(root.getScene().getWindow());if(chosen!=null)logo.setText(chosen.getAbsolutePath());
        })),ipi,button("Salvar configurações",()->{
            try {BigDecimal rate=Store.decimal(ipi.getText());if(name.getText().isBlank()||rate.signum()<0)throw new IllegalArgumentException();
                c.name=name.getText().trim();c.legalName=legal.getText().trim();c.document=doc.getText().trim();
                c.phone=phone.getText().trim();c.email=email.getText().trim();c.website=website.getText().trim();
                c.address=address.getText().trim();
                c.logo=logo.getText().trim();c.ipiPercent=rate;persist();alert("Configurações salvas.");
            }catch(Exception bad){alert("Informe nome e IPI válido.");}
        }));
    }
    private void history() {
        VBox box=page("Histórico de pedidos"); TextField search=field("Pesquisar número ou cliente");
        TableView<Store.Order> table=new TableView<>();table.setPrefHeight(500);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(col("Nº",o->Integer.toString(o.number)),col("Cliente",o->o.clientName),
                col("Data",o->o.date),col("Total",o->Money.money(Store.total(o))));
        Runnable refresh=()->table.setItems(FXCollections.observableArrayList(store.data.orders.stream()
                .filter(o->(o.number+" "+o.clientName).toLowerCase().contains(search.getText().toLowerCase()))
                .sorted((a,b)->Integer.compare(b.number,a.number)).toList()));
        search.textProperty().addListener((o,a,b)->refresh.run());refresh.run();
        table.setRowFactory(v->{TableRow<Store.Order> r=new TableRow<>();r.setOnMouseClicked(e->{if(e.getClickCount()==2&&r.getItem()!=null)order(r.getItem());});return r;});
        box.getChildren().addAll(search,row(button("Abrir / editar",()->{if(selected(table)!=null)order(selected(table));}),
                button("Duplicar",()->{Store.Order old=selected(table);if(old!=null){Store.Order copy=Store.copy(old);copy.id=Store.id();
                    copy.number=store.nextNumber();copy.date=LocalDate.now().toString();order(copy);}}),
                button("Visualizar PDF",()->{if(selected(table)!=null)preview(selected(table));}),
                button("Exportar PDF",()->{if(selected(table)!=null)export(selected(table));}),
                button("Excluir",()->{Store.Order old=selected(table);if(old!=null&&confirm("Excluir pedido Nº "+old.number+"?")){
                    store.data.orders.remove(old);persist();refresh.run();}})),table);
    }
    private void order(Store.Order existing) {
        Store.Order draft=existing==null?new Store.Order():Store.copy(existing);
        if(draft.number==0)draft.number=store.nextNumber();
        if(existing==null)draft.ipiPercent=store.data.company.ipiPercent;
        VBox box=page("Pedido Nº "+draft.number);
        ComboBox<Store.Client> client=new ComboBox<>(FXCollections.observableArrayList(store.data.clients.stream()
                .filter(c->c.active||c.id.equals(draft.clientId)).toList()));client.setPromptText("Cliente");client.setPrefWidth(290);
        store.data.clients.stream().filter(c->c.id.equals(draft.clientId)).findFirst().ifPresent(client::setValue);
        DatePicker date=new DatePicker(LocalDate.parse(draft.date));
        TextField ipi=filled("IPI (%)",draft.ipiPercent.toPlainString());ipi.setPrefWidth(120);
        box.getChildren().add(row(new Label("Cliente"),client,new Label("Data"),date,new Label("IPI"),ipi));
        VBox groups=new VBox(15);Label totals=new Label();totals.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:#176b75;");
        Runnable recalc=()->{try{draft.ipiPercent=Store.decimal(ipi.getText());}catch(Exception bad){draft.ipiPercent=BigDecimal.valueOf(-1);}
            totals.setText("Subtotal "+Money.money(Store.subtotal(draft))+"   IPI "+Money.money(Store.ipi(draft))+"   TOTAL "+Money.money(Store.total(draft)));};
        ipi.textProperty().addListener((o,a,b)->recalc.run());
        Runnable redraw=()->{groups.getChildren().clear();for(Store.Group g:draft.groups)groups.getChildren().add(group(draft,g,groups,recalc));recalc.run();};
        box.getChildren().addAll(button("+ Grupo de material",()->{Store.Group g=new Store.Group();g.items.add(new Store.Item());draft.groups.add(g);redraw.run();}),groups,totals);
        redraw.run();
        TextField weight=filled("Peso (kg) — manual",draft.weight),freight=filled("Frete/condição",draft.freight),
                payment=filled("Forma de pagamento",draft.payment),term=filled("Prazo",draft.paymentTerm),
                pickup=filled("Data de retirada",draft.pickupDate),notes=filled("Observações",draft.notes);
        CheckBox bankToggle=new CheckBox("Incluir dados bancários no PDF");bankToggle.setSelected(draft.includeBank);
        ComboBox<Store.Account> accounts=new ComboBox<>(FXCollections.observableArrayList(store.data.accounts));
        accounts.setPromptText("Conta cadastrada (opcional)");
        TextArea bank=new TextArea(draft.bankText);bank.setPromptText("Dados bancários manuais ou da conta cadastrada");bank.setPrefRowCount(3);
        accounts.setOnAction(e->{Store.Account a=accounts.getValue();if(a!=null)bank.setText(a.bank+" | Agência "+a.agency+" | Conta "+a.number+" | "+a.beneficiary+" | "+a.document+" | PIX "+a.pix);});
        accounts.disableProperty().bind(bankToggle.selectedProperty().not());bank.disableProperty().bind(bankToggle.selectedProperty().not());
        box.getChildren().addAll(new Separator(),new Label("Informações complementares"),weight,freight,payment,term,pickup,notes,bankToggle,accounts,bank);
        Runnable capture=()->{draft.date=date.getValue()==null?"":date.getValue().toString();Store.Client c=client.getValue();
            if(c!=null){draft.clientId=c.id;draft.clientName=c.name;draft.clientDocument=c.document;}
            draft.weight=weight.getText().trim();draft.freight=freight.getText().trim();draft.payment=payment.getText().trim();
            draft.paymentTerm=term.getText().trim();draft.pickupDate=pickup.getText().trim();draft.notes=notes.getText().trim();
            draft.includeBank=bankToggle.isSelected();draft.bankText=bank.getText().trim();};
        box.getChildren().add(row(button("Salvar pedido",()->{capture.run();if(!valid(draft))return;
            int index=store.data.orders.indexOf(existing);if(index<0)store.data.orders.add(draft);else store.data.orders.set(index,draft);persist();history();}),
                button("Visualizar PDF",()->{capture.run();if(valid(draft))preview(draft);}),
                button("Exportar PDF",()->{capture.run();if(valid(draft))export(draft);}),
                button("Cancelar",this::history)));
    }
    private Node group(Store.Order order,Store.Group g,VBox parent,Runnable recalc) {
        VBox card=new VBox(9);card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:#d5e2e8;-fx-border-radius:8;");
        ComboBox<Store.Material> material=new ComboBox<>(FXCollections.observableArrayList(store.data.materials.stream()
                .filter(m->m.active||m.id.equals(g.materialId)).toList()));material.setPromptText("Acabamento");material.setPrefWidth(280);
        store.data.materials.stream().filter(m->m.id.equals(g.materialId)).findFirst().ifPresent(material::setValue);
        TextField price=filled("Preço/m²",g.price.toPlainString());price.setPrefWidth(130);
        material.setOnAction(e->{Store.Material m=material.getValue();if(m!=null){g.materialId=m.id;g.materialName=m.name;g.price=m.price;price.setText(m.price.toPlainString());recalc.run();}});
        price.textProperty().addListener((o,a,b)->{try{g.price=Store.decimal(b);}catch(Exception bad){g.price=BigDecimal.valueOf(-1);}recalc.run();});
        card.getChildren().add(row(material,price,button("Remover grupo",()->{if(confirm("Remover grupo?")){order.groups.remove(g);parent.getChildren().remove(card);recalc.run();}})));
        GridPane grid=new GridPane();grid.setHgap(8);grid.setVgap(8);
        String[] headers={"Comprimento (m)","Largura (m)","Qtd.","Metragem","Valor",""};
        for(int i=0;i<headers.length;i++)grid.add(new Label(headers[i]),i,0);
        Label subtotal=new Label();Runnable summary=()->subtotal.setText("Grupo: "+Money.area(Store.groupArea(g))+" · "+Money.money(Store.groupValue(g)));
        Runnable redraw=new Runnable(){public void run(){
            grid.getChildren().removeIf(n->GridPane.getRowIndex(n)!=null&&GridPane.getRowIndex(n)>0);
            int rowIndex=1;
            for(Store.Item item:new ArrayList<>(g.items)){
                TextField length=filled("0,000",item.length.toPlainString()),width=filled("0,000",item.width.toPlainString()),
                        qty=filled("1",Integer.toString(item.quantity));length.setPrefWidth(115);width.setPrefWidth(115);qty.setPrefWidth(75);
                Label area=new Label(),value=new Label();
                Runnable update=()->{try{item.length=Store.decimal(length.getText());}catch(Exception bad){item.length=BigDecimal.ZERO;}
                    try{item.width=Store.decimal(width.getText());}catch(Exception bad){item.width=BigDecimal.ZERO;}
                    try{item.quantity=Integer.parseInt(qty.getText());}catch(Exception bad){item.quantity=0;}
                    area.setText(Money.area(Store.area(item)));value.setText(Money.money(Store.itemValue(item,g)));summary.run();recalc.run();};
                length.textProperty().addListener((o,a,b)->update.run());width.textProperty().addListener((o,a,b)->update.run());
                qty.textProperty().addListener((o,a,b)->update.run());update.run();
                int r=rowIndex++;grid.add(length,0,r);grid.add(width,1,r);grid.add(qty,2,r);grid.add(area,3,r);grid.add(value,4,r);
                grid.add(button("Excluir",()->{if(confirm("Excluir linha?")){g.items.remove(item);run();summary.run();recalc.run();}}),5,r);
            }
        }};redraw.run();
        price.textProperty().addListener((o,a,b)->summary.run());
        card.getChildren().addAll(grid,button("+ Linha",()->{g.items.add(new Store.Item());redraw.run();summary.run();recalc.run();}),subtotal);
        summary.run();return card;
    }
    private boolean valid(Store.Order o) {
        if(o.clientName.isBlank()||o.date.isBlank()){alert("Selecione cliente e data.");return false;}
        if(o.ipiPercent.signum()<0){alert("IPI inválido.");return false;}
        if(o.groups.isEmpty()){alert("Adicione um grupo e um item.");return false;}
        for(Store.Group g:o.groups){
            if(g.materialName.isBlank()||g.price.signum()<0||g.items.isEmpty()){alert("Cada grupo precisa de acabamento, preço e itens válidos.");return false;}
            for(Store.Item i:g.items)if(i.length.signum()<=0||i.width.signum()<=0||i.quantity<=0){alert("Comprimento, largura e quantidade devem ser positivos.");return false;}
        }return true;
    }
    private void export(Store.Order o){
        FileChooser chooser=new FileChooser();chooser.setInitialFileName("pedido-minastex-"+o.number+".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));
        var chosen=chooser.showSaveDialog(root.getScene().getWindow());
        if(chosen!=null)try{PdfExporter.export(o,store.data.company,chosen.toPath());alert("PDF salvo em "+chosen);}catch(Exception ex){alert("Falha no PDF: "+ex.getMessage());}
    }
    private void preview(Store.Order o){try{Path file=Files.createTempFile("minastex-"+o.number+"-",".pdf");
        PdfExporter.export(o,store.data.company,file);Desktop.getDesktop().open(file.toFile());}
        catch(Exception ex){alert("Falha na prévia: "+ex.getMessage());}}
    private void persist(){try{store.save();}catch(Exception ex){alert("Falha ao salvar: "+ex.getMessage());}}
    private static TextField field(String prompt){TextField t=new TextField();t.setPromptText(prompt);return t;}
    private static TextField filled(String prompt,String value){TextField t=field(prompt);t.setText(value);return t;}
    private static HBox row(Node... nodes){HBox b=new HBox(9,nodes);b.setAlignment(Pos.CENTER_LEFT);return b;}
    private static <T> TableColumn<T,String> col(String title,Function<T,String> f){TableColumn<T,String> c=new TableColumn<>(title);c.setCellValueFactory(x->new ReadOnlyStringWrapper(f.apply(x.getValue())));return c;}
    private static <T> T selected(TableView<T> table){return table.getSelectionModel().getSelectedItem();}
    private static <T> void replace(List<T> list,T old,T value){if(old==null)list.add(value);else list.set(list.indexOf(old),value);}
    private static <T> T clone(Object value,Class<T> type){return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(value,type);}
    private static void alert(String text){new Alert(Alert.AlertType.INFORMATION,text,ButtonType.OK).showAndWait();}
    private static boolean confirm(String text){return new Alert(Alert.AlertType.CONFIRMATION,text,ButtonType.CANCEL,ButtonType.OK).showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
    private static boolean form(String title,TextField... fields){Dialog<ButtonType> d=new Dialog<>();d.setTitle(title);VBox box=new VBox(9,fields);box.setPadding(new Insets(18));
        d.getDialogPane().setContent(box);d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        return d.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
}
