package menus;

public class SubMenu extends Menu {

    public SubMenu(String i_Name) {
        super(i_Name);
        super.setBackOrExitOption("Back");
    }

    @Override
    public void onClick() {
        this.show();
    }
}
