package com.bananarepublic.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public final class Stepper extends HBox {
    private final IntegerProperty value;
    private final int min;
    private final int max;
    private final Button minusBtn;
    private final Button plusBtn;

    public Stepper(int initial, int min, int max) {
        this.min = min;
        this.max = max;
        this.value = new SimpleIntegerProperty(initial);

        getStyleClass().add("stepper");
        setAlignment(Pos.CENTER);

        minusBtn = new Button("−");
        minusBtn.getStyleClass().add("stepper-btn");
        minusBtn.setOnAction(e -> setValue(value.get() - 1));

        Label v = new Label();
        v.getStyleClass().add("stepper-value");
        v.textProperty().bind(value.asString());

        plusBtn = new Button("+");
        plusBtn.getStyleClass().add("stepper-btn");
        plusBtn.setOnAction(e -> setValue(value.get() + 1));

        value.addListener((obs, oldV, newV) -> refresh());
        refresh();
        getChildren().addAll(minusBtn, v, plusBtn);
    }

    public IntegerProperty valueProperty() { return value; }

    public void setValue(int newValue) {
        if (newValue < min) newValue = min;
        if (newValue > max) newValue = max;
        value.set(newValue);
    }

    private void refresh() {
        minusBtn.setDisable(value.get() <= min);
        plusBtn.setDisable(value.get() >= max);
    }
}
