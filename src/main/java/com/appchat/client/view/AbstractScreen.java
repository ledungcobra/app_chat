package com.appchat.client.view;

import com.appchat.utils.Connector;
import com.appchat.utils.ScreenStackManager;
import lombok.val;

import javax.swing.*;
import java.awt.*;
import java.util.Map;


public abstract class AbstractScreen extends JFrame
{
    protected Connector connector;
    protected Map<String, Object> data;


    public AbstractScreen() throws HeadlessException
    {

        connector = new Connector();
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(false);

        this.setLocationRelativeTo(null);
        this.pack();

        try
        {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e)
        {
            System.out.println("Error setting the LAF..." + e);
        }


    }

    @Override
    public void setSize(int width, int height)
    {

        super.setSize(width, height);

        val x = this.getX();
        val y = this.getY();

        this.setLocation(x - width / 2, y - height / 2);
    }

    public abstract void onCreateView();

    public abstract void addEventListener();

    public void setData(Map<String, Object> data)
    {
        this.data = data;
    }

    public Map<String, Object> getData()
    {
        return data;
    }

    protected void finish()
    {
        ScreenStackManager.getInstance().popTopScreen();
    }

}
