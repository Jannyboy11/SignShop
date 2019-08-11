
package org.wargamer2010.signshop.util;

public class SSTimeUtil {
    private SSTimeUtil() {

    }

    /**
     * Returns a string representation of the given amount of seconds
     *
     * @param time Time in seconds
     * @return String representation of the time param
     */
    public static String parseTime(int time) {
        StringBuilder timeString = new StringBuilder();
        TimeUnit[] timeUnits =
                { new TimeUnit(60, "Second", time)
                , new TimeUnit(60, "Minute")
                , new TimeUnit(24, "Hour")
                , new TimeUnit(365, "Day")
                };

        for (int i = 0; i+1 < timeUnits.length; i++) {
            while (timeUnits[i].decrement()) {
                timeUnits[i + 1].increment();
            }
        }

        int temp;
        boolean first = true;
        for (int i = timeUnits.length - 1; i >= 0; i--) {
            temp = timeUnits[i].getAmount();
            if (temp > 0) {
                if (!first)
                    timeString.append(", ");
                else
                    first = false;

                timeString.append(temp).append(" ").append(timeUnits[i].getName());
                if (temp > 1)
                    timeString.append("s");
            }
        }
        int pos = timeString.toString().lastIndexOf(',');
        if (pos >= 0)
            timeString = new StringBuilder(timeString.substring(0, pos) + " and" + timeString.substring(pos + 1));

        return timeString.toString();
    }

    private static class TimeUnit {
        int maxAmount;
        int currentAmount = 0;
        String name;

        TimeUnit(int pMaxAmount, String pName) {
            maxAmount = pMaxAmount;
            name = pName;
        }

        TimeUnit(int pMaxAmount, String pName, int pCurrentAmount) {
            maxAmount = pMaxAmount;
            name = pName;
            currentAmount = pCurrentAmount;
        }

        boolean decrement() {
            if (currentAmount >= maxAmount) {
                currentAmount -= maxAmount;
                return true;
            }

            return false;
        }

        void increment() {
            currentAmount++;
        }

        String getName() {
            return name;
        }

        int getAmount() {
            return currentAmount;
        }

    }
}
