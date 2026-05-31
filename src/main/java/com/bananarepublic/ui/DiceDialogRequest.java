package com.bananarepublic.ui;

public record DiceDialogRequest(
        String title,
        String header,
        String confirmLabel,
        boolean manualEnabled
) {
}
