package net.runelite.client.plugins;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import org.w3c.dom.css.Rect;

import java.awt.*;


@Extension
@PluginDescriptor(
        name = "Bank PIN",
        description = "Automatically enters you bank PIN"
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

    }

    public void makePinNumberOverlay(Graphics2D graphics) {
        if (client.getWidget(WidgetID.BANK_PIN_GROUP_ID, WidgetInfo.BANK_PIN_INSTRUCTION_TEXT.getChildId()) == null ||
                (!FIRST_DIGIT_TEXT.equals(client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText()) &&
                        !SECOND_DIGIT_TEXT.equals(client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText()) &&
                        !THIRD_DIGIT_TEXT.equals(client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText()) &&
                        !FOURTH_DIGIT_TEXT.equals(client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText()))) {
            return;
        }

        char[] pinChars = config.pin().toCharArray();
        if (pinChars.length != 4) {
            return;
        }
        for (char pinChar : pinChars) {
            if (!Character.isDigit(pinChar)) {
                return;
            }
        }

        Rectangle overlayBounds = null;
        String instructionText = client.getWidget(WidgetInfo.BANK_PIN_INSTRUCTION_TEXT).getText();
        if (FIRST_DIGIT_TEXT.equals(instructionText)) {
            overlayBounds = getBoundsForPinDigit(Character.digit(pinChars[0], 10));
        } else if (SECOND_DIGIT_TEXT.equals(instructionText)) {
            overlayBounds = getBoundsForPinDigit(Character.digit(pinChars[1], 10));
        } else if (THIRD_DIGIT_TEXT.equals(instructionText)) {
            overlayBounds = getBoundsForPinDigit(Character.digit(pinChars[2], 10));
        } else if (FOURTH_DIGIT_TEXT.equals(instructionText)) {
            overlayBounds = getBoundsForPinDigit(Character.digit(pinChars[3], 10));
        }

        if (overlayBounds == null) {
            return;
        }

        graphics.setColor(new Color(0, 255, 255, 65));
        graphics.fill(overlayBounds.getBounds());
        graphics.setColor(new Color(0, 255, 255));
        graphics.draw(overlayBounds.getBounds());
    }


    private Rectangle getBoundsForPinDigit(int digit) {
        for (WidgetInfo info : BANK_PIN_WIDGET_INFOS) {
            Widget widget = client.getWidget(info);
            if (widget == null) {
                continue;
            }

            Widget[] children = widget.getChildren();
            for (Widget child : children) {
                if (String.valueOf(digit).equals(child.getText())) {
                    return widget.getBounds();
                }
            }
        }
        return null;
    }
}