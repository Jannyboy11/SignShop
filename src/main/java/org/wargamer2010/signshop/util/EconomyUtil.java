package org.wargamer2010.signshop.util;

import org.bukkit.ChatColor;
import org.wargamer2010.signshop.Vault;

public class EconomyUtil {
    private static ChatColor moneyColor = ChatColor.GREEN;

    private EconomyUtil() {

    }

    private static String attachColor(String money) {
        return (moneyColor + money + ChatColor.WHITE);
    }

    public static String formatMoney(double money) {
        if (Vault.getEconomy() == null)
            return attachColor(Double.toString(money));
        else
            return attachColor(Vault.getEconomy().format(money));
    }

    public static double parsePrice(String line) {
        if (line == null) return 0.0d;

        String priceLine = ChatColor.stripColor(line);
        StringBuilder sPrice = new StringBuilder();
        double fPrice;
        for (int i = 0; i < priceLine.length(); i++)
            if (Character.isDigit(priceLine.charAt(i)) || priceLine.charAt(i) == '.')
                sPrice.append(priceLine.charAt(i));
        try {
            fPrice = Double.parseDouble(sPrice.toString());
        } catch (NumberFormatException nfe) {
            fPrice = 0.0d;
        }
        if (fPrice < 0.0f) {
            fPrice = 0.0d;
        }

        Double doubleObject = Double.valueOf(fPrice);
        if (doubleObject.isNaN() || doubleObject.isInfinite())
            fPrice = 0.0d;

        return fPrice;
    }

}
