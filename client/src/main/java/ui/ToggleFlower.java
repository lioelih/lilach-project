package ui;

import il.cshaifasweng.OCSFMediatorExample.entities.Product;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

public class ToggleFlower extends VBox {
    private final Product product;
    private final CheckBox toggle = new CheckBox();
    private Spinner<Integer> qtySpinner = null;
    private Consumer<Boolean> onToggled;

    public ToggleFlower(Product product) {
        this.product = product;
        setAlignment(Pos.TOP_CENTER);
        setSpacing(4);

        // 1) flower icon
        if (product.getImage() != null) {
            ImageView iv = new ImageView(
                    new Image(new ByteArrayInputStream(product.getImage()))
            );
            iv.setFitWidth(48);
            iv.setFitHeight(48);
            getChildren().add(iv);
        }

        // 2) checkbox labeled by product name
        toggle.setText(product.getName());
        toggle.selectedProperty().addListener((obs, oldV, newV) -> {
            // show or hide the spinner, but KEEP it in the layout
            qtySpinner.setVisible(newV);
            if (onToggled != null) onToggled.accept(newV);
        });
        getChildren().add(toggle);

        // 3) the spinner is always a child — just invisible until needed
        qtySpinner = new Spinner<>();
        qtySpinner.setVisible(false);
        qtySpinner.setPrefWidth(52);
        qtySpinner.setEditable(true);
        // default factory; will be replaced by showQuantitySelector()
        qtySpinner.setValueFactory(new IntegerSpinnerValueFactory(1, 50, 1));
        // clamp text on focus‐lost
        qtySpinner.focusedProperty().addListener((obs, wasF, isF) -> {
            if (!isF) {
                IntegerSpinnerValueFactory f =
                        (IntegerSpinnerValueFactory) qtySpinner.getValueFactory();
                String txt = qtySpinner.getEditor().getText();
                int v;
                try {
                    v = Integer.parseInt(txt);
                } catch (NumberFormatException ex) {
                    v = f.getMin();
                }
                // clamp
                v = Math.max(f.getMin(), Math.min(f.getMax(), v));
                f.setValue(v);
            }
        });
        getChildren().add(qtySpinner);

        getStyleClass().add("toggle-flower");
    }

    /** Called by controller to listen for toggle on/off */
    public void setOnToggled(Consumer<Boolean> c) {
        this.onToggled = c;
    }

    /**
     * Show and configure the spinner
     * @param initial the starting value
     * @param max the maximum allowed value
     * @param onAmountChanged callback when user picks a new number
     */
    public void showQuantitySelector(int initial,
                                     int max,
                                     Consumer<Integer> onAmountChanged) {
        IntegerSpinnerValueFactory f =
                new IntegerSpinnerValueFactory(1, max, initial);
        f.valueProperty().addListener((obs, oldV, newV) -> onAmountChanged.accept(newV));
        qtySpinner.setValueFactory(f);
        qtySpinner.setVisible(true);
    }

    /** Hide (but do not remove) the spinner */
    public void removeQuantitySelector() {
        qtySpinner.setVisible(false);
    }

    /** For controller to read the chosen quantity */
    public int getQuantity() {
        return qtySpinner.isVisible() ? qtySpinner.getValue() : 0;
    }

    /** For controller’s dynamic‐limit logic */
    public Spinner<Integer> getSpinner() {
        return qtySpinner;
    }

    /** For controller’s dynamic‐limit logic */
    public boolean isSelected() {
        return toggle.isSelected();
    }

    /** Restore an existing quantity (when loading a saved bouquet) */
    public void setSelectedQuantity(int qty) {
        toggle.setSelected(true);
        // spinner visible ⇒ factory will be set on next updateQuantityLimits()
        showQuantitySelector(qty, 50, v -> {
            if (onToggled != null) onToggled.accept(true);
        });
    }

    /** Deselect this flower and hide the spinner */
    public void deselect() {
        toggle.setSelected(false);
        removeQuantitySelector();
    }

    public Product getProduct() {
        return product;
    }
    public CheckBox getToggle() {
        return toggle;
    }
}
