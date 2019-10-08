package menus;

import magit.IEngine;

public class MainMenu extends Menu {

    public MainMenu(String i_Name, IEngine i_Engine) {
        super(i_Name);
        super.setBackOrExitOption("Exit");
        setEngine(i_Engine);
    }

    @Override
    public void show() {
        super.show();
        System.out.println("Goodbye!");
    }

    @Override
    public void onClick() {}

    @Override
    public void addItem(Menu i_ToAdd) {
        getItems().add(i_ToAdd);
        i_ToAdd.setEngine(getEngine());
    }
}
