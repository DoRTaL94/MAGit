package menus;

import IO.*;
import magit.IEngine;
import java.util.ArrayList;
import java.util.List;

public abstract class Menu {
    private static IEngine engine;
    private final List<Menu> items;
    private String name;
    private boolean isBackOrExit;
    private String backOrExitOption;
    private int userChoice;

    public Menu(String i_Name){
        name = i_Name;
        items = new ArrayList<>();
        isBackOrExit = false;
        backOrExitOption = null;
        userChoice = -1;
    }

    public abstract void onClick();

    public void show() {
        int i;
        List<Menu> items = getItems();

        while(!isBackOrExit()) {
            System.out.format("%s \\ User: %s \\ Repository: %s >%s",
                getName(),
                    engine.getCurrentUserName(),
                    engine.getActiveRepository() == null ? "N/A" : engine.getActiveRepository().getName(),
                System.lineSeparator());

            for(i = 0; i< items.size(); i++) {
                System.out.format("%d. %s%n", i + 1, items.get(i).getName());
            }

            System.out.format("%d. %s%n", i + 1, backOrExitOption);

            try {
                userChoice = ConsoleUtils.getUserChoice();
                System.out.println();

                if (userChoice == i + 1) {
                    setIsBackOrExit(true);
                } else if (userChoice > 0 && userChoice <= i) {
                    items.get(userChoice - 1).onClick();
                } else {
                    System.out.format("ERROR: Expected input is a number between %d and %d. Please try again.", 1, i + 1);
                    System.out.println();
                    System.out.println();
                }
            } catch (NumberFormatException nfe) {
                System.out.println();
                System.out.println("ERROR: Input is not a number! Please try again.");
                System.out.println();
            }
        }
    }

    public final List<Menu> getItems() {
        return items;
    }

    public void addItem(Menu i_ToAdd){
        items.add(i_ToAdd);
    }

    public String getName() {
        return name;
    }

    public boolean isBackOrExit() {
        return isBackOrExit;
    }

    public void setIsBackOrExit(boolean i_IsBackOrExit) {
        isBackOrExit = i_IsBackOrExit;
    }

    public void setBackOrExitOption(String i_Option) {
        backOrExitOption = i_Option;
    }

    public static IEngine getEngine() {
        return engine;
    }

    public static void setEngine(IEngine i_Engine) {
        engine = i_Engine;
    }

    private void checkIfInputExists() {
        while(userChoice == -1) {

        }

        this.notifyAll();
    }
}
