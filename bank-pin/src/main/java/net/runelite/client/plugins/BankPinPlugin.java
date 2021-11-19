package net.runelite.client.plugins;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;


@Extension
@PluginDescriptor(
        name = "Bank PIN",
        description = "Bank PIN utilities"
)
@Slf4j
public class BankPinPlugin extends Plugin {
    private static final String FIRST_DIGIT_TEXT = "First click the FIRST digit.";
    private static final String SECOND_DIGIT_TEXT = "Now click the SECOND digit.";
    private static final String THIRD_DIGIT_TEXT = "Time for the THIRD digit.";
    private static final String FOURTH_DIGIT_TEXT = "Finally, the FOURTH digit.";

    private static final WidgetInfo[] BANK_PIN_WIDGET_INFOS = {
            WidgetInfo.BANK_PIN_1,
            WidgetInfo.BANK_PIN_2,
            WidgetInfo.BANK_PIN_3,
            WidgetInfo.BANK_PIN_4,
            WidgetInfo.BANK_PIN_5,
            WidgetInfo.BANK_PIN_6,
            WidgetInfo.BANK_PIN_7,
            WidgetInfo.BANK_PIN_8,
            WidgetInfo.BANK_PIN_9,
            WidgetInfo.BANK_PIN_10
    };

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PinNumberOverlay pinNumberOverlay;

    @Inject
    private BankPinConfig config;

    @Provides
    BankPinConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BankPinConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(pinNumberOverlay);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(pinNumberOverlay);
    }

    @Subscribe
    private void onGameTick(GameTick gameTick) {
        if (!config.autoEnterPin()) {
            return;
        }

        int nextDigitToEnter = getNextDigitToEnter();
        if (nextDigitToEnter < 0 || nextDigitToEnter > 9) {
            return;
        }

        typePinDigit(nextDigitToEnter);
    }

    public void makePinNumberOverlay(Graphics2D graphics) {
        if (!config.highlightNextPin()) {
            return;
        }

        int nextDigitToEnter = getNextDigitToEnter();
        if (nextDigitToEnter < 0 || nextDigitToEnter > 9) {
            return;
        }

        Widget targetWidget = getWidgetForPinDigit(nextDigitToEnter);
        if (targetWidget == null) {
            return;
        }

        Rectangle overlayBounds = targetWidget.getBounds();
        if (overlayBounds == null) {
            return;
        }

        graphics.setColor(new Color(0, 255, 255, 65));
        graphics.fill(overlayBounds.getBounds());
        graphics.setColor(new Color(0, 255, 255));
        graphics.draw(overlayBounds.getBounds());
    }

    private Widget getWidgetForPinDigit(int digit) {
        HashMap<Integer, Widget> digitToWidget = new HashMap<Integer, Widget>();
        for (WidgetInfo info : BANK_PIN_WIDGET_INFOS) {
            Widget widget = client.getWidget(info);
            if (widget == null) {
                continue;
            }

            Widget[] children = widget.getChildren();
            for (Widget child : children) {
                try {
                    int parsedDigit = Integer.parseInt(child.getText());
                    digitToWidget.put(parsedDigit, widget);
                } catch (NumberFormatException ex) {
                    // No number is visible on the widget if the mouse is over it.
                    // In case this one is our target and we haven't come across our target yet, stick it in the map.
                    // If we come across our digit later it will simply overwrite this and everything will still work.
                    if (!digitToWidget.containsKey(digit)) {
                        digitToWidget.put(digit, widget);
                    }
                }
            }
        }

        return digitToWidget.get(digit);
    }

    private int getNextDigitToEnter() {
        if (client.getWidget(WidgetID.BANK_PIN_GROUP_ID, WidgetInfo.BANK_PIN_INSTRUCTION_TEXT.getChildId()) == null ||
                (!FIRST_DIGIT_TEXT.equals(client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText()) &&
                        !SECOND_DIGIT_TEXT.equals(client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText()) &&
                        !THIRD_DIGIT_TEXT.equals(client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText()) &&
                        !FOURTH_DIGIT_TEXT.equals(client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText()))) {
            return -1;
        }

        char[] pinChars = config.pin().toCharArray();
        if (!isPinValid(pinChars)) {
            return -1;
        }

        String instructionText = client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText();
        if (FIRST_DIGIT_TEXT.equals(instructionText)) {
            return Character.digit(pinChars[0], 10);
        } else if (SECOND_DIGIT_TEXT.equals(instructionText)) {
            return Character.digit(pinChars[1], 10);
        } else if (THIRD_DIGIT_TEXT.equals(instructionText)) {
            return Character.digit(pinChars[2], 10);
        } else if (FOURTH_DIGIT_TEXT.equals(instructionText)) {
            return Character.digit(pinChars[3], 10);
        } else {
            return -1;
        }
    }

    private boolean isPinValid(char[] pinChars) {
        if (pinChars.length != 4) {
            return false;
        }
        for (char pinChar : pinChars) {
            if (!Character.isDigit(pinChar)) {
                return false;
            }
        }
        return true;
    }

    private void typePinDigit(int digit) {
        KeyEvent keyPress = new KeyEvent(client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.getExtendedKeyCodeForChar(digit), KeyEvent.CHAR_UNDEFINED);
        KeyEvent keyType = new KeyEvent(client.getCanvas(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, Character.forDigit(digit, 10));
        KeyEvent keyRelease = new KeyEvent(client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.getExtendedKeyCodeForChar(digit), KeyEvent.CHAR_UNDEFINED);

        client.getCanvas().dispatchEvent(keyPress);
        client.getCanvas().dispatchEvent(keyType);
        client.getCanvas().dispatchEvent(keyRelease);
    }
}