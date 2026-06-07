package com.bananarepublic.ui;

import com.bananarepublic.service.dice.DiceMode;
import com.bananarepublic.service.dice.DiceRoll;

public record DiceDialogResult(DiceMode mode, DiceRoll manualRoll) {
}
