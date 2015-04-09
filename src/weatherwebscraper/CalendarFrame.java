/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weatherwebscraper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jdesktop.swingx.JXDatePicker;

/**
 *
 * @author 422
 */
public class CalendarFrame extends JFrame implements ActionListener {

    JXDatePicker picker;
    WeatherWebScraper parent;
    
    public CalendarFrame(WeatherWebScraper par) {
        
        parent = par;
        
        JPanel panel = new JPanel();
        JButton button = new JButton("OK");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(400, 400, 250, 100);

        picker = new JXDatePicker();
        picker.setDate(Calendar.getInstance().getTime());
        picker.setFormats(new SimpleDateFormat("dd.MM.yyyy"));
        

        panel.add(picker);
        panel.add(button);
        getContentPane().add(panel);

        button.addActionListener(this);
        setVisible(true);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        
        parent.ParseDateToCSV( picker.getDate() );
        dispose();
    }
    
}
